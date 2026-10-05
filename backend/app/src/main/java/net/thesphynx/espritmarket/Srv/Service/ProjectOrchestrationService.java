package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Srv.Dto.*;
import net.thesphynx.espritmarket.Srv.Entity.*;
import net.thesphynx.espritmarket.Srv.Mapper.ProjectDependencyMapper;
import net.thesphynx.espritmarket.Srv.Mapper.ProjectMilestoneMapper;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectDependencyRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectMilestoneRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import net.thesphynx.espritmarket.Srv.Repository.IMilestoneServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ProjectOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ProjectOrchestrationService.class);

    private final IProjectRepository projectRepository;
    private final IProjectMilestoneRepository milestoneRepository;
    private final IProjectDependencyRepository dependencyRepository;
    private final IBookingRepository bookingRepository;
    private final IServiceRepository serviceRepository;
    private final ProjectMilestoneMapper milestoneMapper;
    private final ProjectDependencyMapper dependencyMapper;
    private final SlotScoringService slotScoringService;
    private final SlotAllocationAuditService slotAllocationAuditService;
    private final ProjectService projectService;
    private final AvailabilityService availabilityService;
    private final IMilestoneServiceRepository milestoneServiceRepository;

    public ProjectOrchestrationService(IProjectRepository projectRepository,
                                        IProjectMilestoneRepository milestoneRepository,
                                        IProjectDependencyRepository dependencyRepository,
                                        IBookingRepository bookingRepository,
                                        IServiceRepository serviceRepository,
                                        ProjectMilestoneMapper milestoneMapper,
                                        ProjectDependencyMapper dependencyMapper,
                                        SlotScoringService slotScoringService,
                                        SlotAllocationAuditService slotAllocationAuditService,
                                        ProjectService projectService,
                                        AvailabilityService availabilityService,
                                        IMilestoneServiceRepository milestoneServiceRepository) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.dependencyRepository = dependencyRepository;
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.milestoneMapper = milestoneMapper;
        this.dependencyMapper = dependencyMapper;
        this.slotScoringService = slotScoringService;
        this.slotAllocationAuditService = slotAllocationAuditService;
        this.projectService = projectService;
        this.availabilityService = availabilityService;
        this.milestoneServiceRepository = milestoneServiceRepository;
    }

    public List<ProjectMilestoneResponse> getMilestones(Long projectId) {
        ensureProjectExists(projectId);
        return milestoneRepository.findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId).stream()
                .map(milestoneMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProjectMilestoneResponse createMilestone(Long projectId, ProjectMilestoneRequest request) {
        Project project = ensureProjectExists(projectId);
        validateMilestoneDates(request);

        ProjectMilestone milestone = milestoneMapper.toEntity(request, project);
        ProjectMilestone saved = milestoneRepository.save(milestone);
        return milestoneMapper.toResponse(saved);
    }

    @Transactional
    public ProjectMilestoneResponse updateMilestone(Long projectId, Long milestoneId, ProjectMilestoneRequest request) {
        ensureProjectExists(projectId);
        validateMilestoneDates(request);

        ProjectMilestone milestone = milestoneRepository.findById(milestoneId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", milestoneId));

        if (!milestone.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Milestone does not belong to this project");
        }

        milestoneMapper.applyRequest(milestone, request);
        ProjectMilestone saved = milestoneRepository.save(milestone);

        if (saved.getStatus() == ProjectMilestoneStatus.COMPLETED) {
            projectService.autoTransitionIfAllMilestonesCompleted(projectId);
        }

        return milestoneMapper.toResponse(saved);
    }

    @Transactional
    public void deleteMilestone(Long projectId, Long milestoneId) {
        ensureProjectExists(projectId);

        ProjectMilestone milestone = milestoneRepository.findById(milestoneId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", milestoneId));

        if (!milestone.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Milestone does not belong to this project");
        }

        milestone.setDeletedAt(java.time.LocalDateTime.now());
        milestoneRepository.save(milestone);
    }

    @Transactional
    public List<ProjectMilestoneResponse> reorderMilestones(Long projectId, List<Long> orderedIds) {
        ensureProjectExists(projectId);

        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);

        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            ProjectMilestone milestone = milestones.stream()
                    .filter(m -> m.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", id));
            milestone.setSortOrder(i);
            milestoneRepository.save(milestone);
        }

        return milestoneRepository.findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId)
                .stream().map(milestoneMapper::toResponse).toList();
    }

    @Transactional
    public ProjectMilestoneResponse linkBooking(Long projectId, Long milestoneId, Long bookingId) {
        ensureProjectExists(projectId);

        ProjectMilestone milestone = milestoneRepository.findById(milestoneId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", milestoneId));

        if (!milestone.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Milestone does not belong to this project");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getProject() == null || !booking.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Booking does not belong to this project");
        }

        milestone.getBookings().add(booking);
        return milestoneMapper.toResponse(milestoneRepository.save(milestone));
    }

    @Transactional
    public ProjectMilestoneResponse unlinkBooking(Long projectId, Long milestoneId, Long bookingId) {
        ensureProjectExists(projectId);

        ProjectMilestone milestone = milestoneRepository.findById(milestoneId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", milestoneId));

        if (!milestone.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Milestone does not belong to this project");
        }

        milestone.getBookings().removeIf(b -> b.getId().equals(bookingId));
        return milestoneMapper.toResponse(milestoneRepository.save(milestone));
    }

    public List<ProjectDependencyResponse> getDependencies(Long projectId) {
        ensureProjectExists(projectId);
        return dependencyRepository.findByProjectIdOrderByIdAsc(projectId).stream()
                .map(dependencyMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProjectDependencyResponse createDependency(Long projectId, ProjectDependencyRequest request) {
        Project project = ensureProjectExists(projectId);

        if (request.getPredecessorMilestoneId().equals(request.getSuccessorMilestoneId())) {
            throw new BadRequestException("A milestone cannot depend on itself");
        }

        ProjectMilestone predecessor = findMilestoneForProject(projectId, request.getPredecessorMilestoneId());
        ProjectMilestone successor = findMilestoneForProject(projectId, request.getSuccessorMilestoneId());

        ProjectDependency dependency = new ProjectDependency();
        dependency.setProject(project);
        dependency.setPredecessorMilestone(predecessor);
        dependency.setSuccessorMilestone(successor);

        ProjectDependency saved = dependencyRepository.save(dependency);
        return dependencyMapper.toResponse(saved);
    }

    @Transactional
    public void deleteDependency(Long projectId, Long dependencyId) {
        ensureProjectExists(projectId);

        ProjectDependency dependency = dependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectDependency", dependencyId));

        if (!dependency.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Dependency does not belong to this project");
        }

        dependencyRepository.delete(dependency);
    }

    @Transactional
    public List<ProjectDependencyResponse> bulkApplyDependencies(Long projectId, List<ProjectDependencyRequest> requests) {
        Project project = ensureProjectExists(projectId);
        List<ProjectDependency> existing = dependencyRepository.findByProjectIdOrderByIdAsc(projectId);
        Set<String> existingPairs = existing.stream()
                .map(d -> d.getPredecessorMilestone().getId() + "->" + d.getSuccessorMilestone().getId())
                .collect(Collectors.toSet());

        List<ProjectDependency> saved = new ArrayList<>();
        Map<Long, ProjectMilestone> milestoneMap = new java.util.HashMap<>();
        for (ProjectMilestone m : milestoneRepository.findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId)) {
            milestoneMap.put(m.getId(), m);
        }

        for (ProjectDependencyRequest req : requests) {
            if (req.getPredecessorMilestoneId() == null || req.getSuccessorMilestoneId() == null) continue;
            if (req.getPredecessorMilestoneId().equals(req.getSuccessorMilestoneId())) continue;
            String pairKey = req.getPredecessorMilestoneId() + "->" + req.getSuccessorMilestoneId();
            if (existingPairs.contains(pairKey)) continue;
            if (!milestoneMap.containsKey(req.getPredecessorMilestoneId()) || !milestoneMap.containsKey(req.getSuccessorMilestoneId())) continue;

            ProjectDependency dep = new ProjectDependency();
            dep.setProject(project);
            dep.setPredecessorMilestone(milestoneMap.get(req.getPredecessorMilestoneId()));
            dep.setSuccessorMilestone(milestoneMap.get(req.getSuccessorMilestoneId()));
            saved.add(dependencyRepository.save(dep));
            existingPairs.add(pairKey);
        }

        return saved.stream().map(dependencyMapper::toResponse).collect(Collectors.toList());
    }

    public ProjectTimelineResponse getTimeline(Long projectId) {
        Project project = ensureProjectExists(projectId);
        List<ProjectMilestone> milestones = milestoneRepository.findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);
        List<ProjectDependency> dependencies = dependencyRepository.findByProjectIdOrderByIdAsc(projectId);

        int total = milestones.size();
        int completed = (int) milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.COMPLETED).count();
        int blocked = (int) milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.BLOCKED).count();
        double percent = total == 0 ? 0.0 : (completed * 100.0) / total;

        ProjectTimelineResponse response = new ProjectTimelineResponse();
        response.setProjectId(project.getId());
        response.setProjectTitle(project.getTitle());
        response.setMilestones(milestones.stream().map(milestoneMapper::toResponse).toList());
        response.setDependencies(dependencies.stream().map(dependencyMapper::toResponse).toList());
        response.setTotalMilestones(total);
        response.setCompletedMilestones(completed);
        response.setBlockedMilestones(blocked);
        response.setCompletionPercent(percent);
        return response;
    }

    public SlotSuggestionResponse suggestSlotsForProject(Long projectId,
                                                          Long serviceId,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          SlotScoringMode mode,
                                                          int limit) {
        ensureProjectExists(projectId);
        SlotSuggestionResponse response = slotScoringService.suggestSlots(
                serviceId,
                startDate,
                endDate,
                projectId,
                mode,
                limit
        );
        slotAllocationAuditService.recordTopSuggestions(response);
        return response;
    }

    @Transactional
    public ProjectTimelineResponse executeWorkflow(Long projectId) {
        Project project = ensureProjectExists(projectId);
        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);

        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("project", project);

        for (ProjectMilestone milestone : milestones) {
            if (milestone.getStatus() == ProjectMilestoneStatus.COMPLETED ||
                    milestone.getStatus() == ProjectMilestoneStatus.CANCELLED) {
                continue;
            }

            if (hasCancelledPredecessor(projectId, milestone.getId())) {
                milestone.setStatus(ProjectMilestoneStatus.CANCELLED);
                milestoneRepository.save(milestone);
                continue;
            }

            boolean blocked = hasIncompletePredecessor(projectId, milestone.getId());
            ProjectMilestoneStatus target = blocked ? ProjectMilestoneStatus.BLOCKED : ProjectMilestoneStatus.PLANNED;

            if (milestone.getMilestoneType() == MilestoneType.CONDITION) {
                if (!blocked && milestone.getConditionExpression() != null && !milestone.getConditionExpression().isBlank()) {
                    try {
                        Boolean result = parser.parseExpression(milestone.getConditionExpression()).getValue(context, Boolean.class);
                        if (Boolean.FALSE.equals(result)) {
                            // Condition is false, skip this branch
                            milestone.setStatus(ProjectMilestoneStatus.CANCELLED);
                            milestoneRepository.save(milestone);
                            continue;
                        } else {
                            // Condition met, auto-complete so downstream can proceed
                            milestone.setStatus(ProjectMilestoneStatus.COMPLETED);
                            milestoneRepository.save(milestone);
                            continue;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to evaluate SpEL condition '{}' for milestone {}: {}", milestone.getConditionExpression(), milestone.getId(), e.getMessage());
                    }
                }
            }

            if (milestone.getStatus() == ProjectMilestoneStatus.BLOCKED && !blocked) {
                milestone.setStatus(ProjectMilestoneStatus.PLANNED);
            } else if (milestone.getStatus() == ProjectMilestoneStatus.PLANNED && blocked) {
                milestone.setStatus(ProjectMilestoneStatus.BLOCKED);
            }

            if (milestone.getStatus() != target && milestone.getStatus() != ProjectMilestoneStatus.IN_PROGRESS && milestone.getStatus() != ProjectMilestoneStatus.CANCELLED) {
                milestone.setStatus(target);
            }

            if (milestone.getStatus() == ProjectMilestoneStatus.PLANNED
                    && !blocked
                    && allLinkedBookingsStarted(milestone)) {
                milestone.setStatus(ProjectMilestoneStatus.IN_PROGRESS);
                if (milestone.getActualStartDate() == null) {
                    milestone.setActualStartDate(new java.util.Date());
                }
            }

            if (milestone.getStatus() == ProjectMilestoneStatus.IN_PROGRESS && allLinkedBookingsCompleted(milestone)) {
                milestone.setStatus(ProjectMilestoneStatus.COMPLETED);
                if (milestone.getActualEndDate() == null) {
                    milestone.setActualEndDate(new java.util.Date());
                }
            }

            milestoneRepository.save(milestone);
        }

        projectService.autoTransitionIfAllMilestonesCompleted(projectId);
        return getTimeline(projectId);
    }

    @Transactional
    public ProjectTimelineResponse replanProject(Long projectId) {
        ensureProjectExists(projectId);
        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);

        java.time.LocalDate cursor = java.time.LocalDate.now();

        for (ProjectMilestone milestone : milestones) {
            if (milestone.getStatus() == ProjectMilestoneStatus.COMPLETED ||
                    milestone.getStatus() == ProjectMilestoneStatus.CANCELLED) {
                continue;
            }

            java.time.LocalDate earliest = cursor;
            List<ProjectDependency> incoming = dependencyRepository
                    .findByProjectIdAndSuccessorMilestoneId(projectId, milestone.getId());

            for (ProjectDependency dep : incoming) {
                ProjectMilestone pred = dep.getPredecessorMilestone();
                if (pred.getActualEndDate() != null) {
                    java.time.LocalDate predEnd = pred.getActualEndDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    if (predEnd.plusDays(1).isAfter(earliest)) {
                        earliest = predEnd.plusDays(1);
                    }
                } else if (pred.getPlannedEndDate() != null) {
                    java.time.LocalDate predEnd = pred.getPlannedEndDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    if (predEnd.plusDays(1).isAfter(earliest)) {
                        earliest = predEnd.plusDays(1);
                    }
                }
            }

            long durationDays = 3;
            if (milestone.getPlannedStartDate() != null && milestone.getPlannedEndDate() != null) {
                java.time.LocalDate s = milestone.getPlannedStartDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                java.time.LocalDate e = milestone.getPlannedEndDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                durationDays = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(s, e) + 1);
            }

            java.time.LocalDate plannedStart = earliest;
            java.time.LocalDate plannedEnd = plannedStart.plusDays((long) durationDays - 1);

            milestone.setPlannedStartDate(java.util.Date.from(plannedStart.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
            milestone.setPlannedEndDate(java.util.Date.from(plannedEnd.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
            milestoneRepository.save(milestone);

            cursor = plannedEnd.plusDays(1);
        }

        return getTimeline(projectId);
    }

    private boolean hasCancelledPredecessor(Long projectId, Long milestoneId) {
        List<ProjectDependency> incoming = dependencyRepository
                .findByProjectIdAndSuccessorMilestoneId(projectId, milestoneId);
        return incoming.stream()
                .map(ProjectDependency::getPredecessorMilestone)
                .anyMatch(pred -> pred.getStatus() == ProjectMilestoneStatus.CANCELLED);
    }

    private boolean hasIncompletePredecessor(Long projectId, Long milestoneId) {
        List<ProjectDependency> incoming = dependencyRepository
                .findByProjectIdAndSuccessorMilestoneId(projectId, milestoneId);
        return incoming.stream()
                .map(ProjectDependency::getPredecessorMilestone)
                .anyMatch(pred -> pred.getStatus() != ProjectMilestoneStatus.COMPLETED && pred.getStatus() != ProjectMilestoneStatus.CANCELLED);
    }

    private boolean allLinkedBookingsStarted(ProjectMilestone milestone) {
        if (milestone.getBookings() == null || milestone.getBookings().isEmpty()) {
            return true;
        }
        return milestone.getBookings().stream().allMatch(b ->
                b.getStatus() == BookingStatus.IN_PROGRESS ||
                b.getStatus() == BookingStatus.PENDING_REVIEW ||
                b.getStatus() == BookingStatus.COMPLETED
        );
    }

    private boolean allLinkedBookingsCompleted(ProjectMilestone milestone) {
        if (milestone.getBookings() == null || milestone.getBookings().isEmpty()) {
            return false;
        }
        return milestone.getBookings().stream().allMatch(b -> b.getStatus() == BookingStatus.COMPLETED);
    }

    private Project ensureProjectExists(Long projectId) {
        return projectRepository.findById(projectId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    private ProjectMilestone findMilestoneForProject(Long projectId, Long milestoneId) {
        ProjectMilestone milestone = milestoneRepository.findById(milestoneId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", milestoneId));

        if (!milestone.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Milestone does not belong to this project");
        }
        return milestone;
    }

    private void validateMilestoneDates(ProjectMilestoneRequest request) {
        if (request.getPlannedStartDate() != null && request.getPlannedEndDate() != null
                && request.getPlannedStartDate().after(request.getPlannedEndDate())) {
            throw new BadRequestException("Planned start date must be before planned end date");
        }

        if (request.getActualStartDate() != null && request.getActualEndDate() != null
                && request.getActualStartDate().after(request.getActualEndDate())) {
            throw new BadRequestException("Actual start date must be before actual end date");
        }
    }

    @Transactional
    public ProjectMilestoneResponse linkService(Long projectId, Long milestoneId, Long serviceId) {
        ensureProjectExists(projectId);
        ProjectMilestone milestone = findMilestoneForProject(projectId, milestoneId);
        Service service = serviceRepository.findById(serviceId)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));

        if (!service.isAllowProjectParticipation()) {
            throw new BadRequestException("This service does not allow project participation");
        }

        boolean alreadyLinked = milestone.getMilestoneServices().stream()
                .anyMatch(ms -> ms.getService() != null && ms.getService().getId().equals(serviceId));
        if (!alreadyLinked) {
            MilestoneService ms = new MilestoneService();
            ms.setMilestoneId(milestone.getId());
            ms.setServiceId(service.getId());
            ms.setMilestone(milestone);
            ms.setService(service);
            ms.setEstimatedHours(2.0);
            milestone.getMilestoneServices().add(ms);
        }
        return milestoneMapper.toResponse(milestoneRepository.save(milestone));
    }

    @Transactional
    public ProjectMilestoneResponse unlinkService(Long projectId, Long milestoneId, Long serviceId) {
        ensureProjectExists(projectId);
        ProjectMilestone milestone = findMilestoneForProject(projectId, milestoneId);
        milestone.getMilestoneServices().removeIf(ms -> ms.getService() != null && ms.getService().getId().equals(serviceId));
        return milestoneMapper.toResponse(milestoneRepository.save(milestone));
    }

    public ProjectScheduleResponse generateSchedule(Long projectId) {
        Project project = ensureProjectExists(projectId);
        try {
            return doGenerateSchedule(project, projectId);
        } catch (Exception e) {
            log.error("Failed to generate schedule for project {}: {}", projectId, e.getMessage(), e);
            ProjectScheduleResponse fallback = new ProjectScheduleResponse();
            fallback.setProjectId(projectId);
            return fallback;
        }
    }

    private ProjectScheduleResponse doGenerateSchedule(Project project, Long projectId) {
        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);
        List<ProjectDependency> dependencies = dependencyRepository.findByProjectIdOrderByIdAsc(projectId);

        Map<Long, List<Long>> adjacency = new HashMap<>();
        for (ProjectDependency dep : dependencies) {
            adjacency.computeIfAbsent(dep.getPredecessorMilestone().getId(), k -> new ArrayList<>())
                    .add(dep.getSuccessorMilestone().getId());
        }

        Map<Long, Integer> depthMap = new HashMap<>();
        for (ProjectMilestone m : milestones) {
            computeDepth(m.getId(), adjacency, depthMap, new HashSet<>());
        }

        Map<Long, Long> earliestPredecessorEnd = new HashMap<>();
        for (ProjectDependency dep : dependencies) {
            Long predId = dep.getPredecessorMilestone().getId();
            Long succId = dep.getSuccessorMilestone().getId();
            ProjectMilestone pred = milestones.stream()
                    .filter(m -> m.getId().equals(predId)).findFirst().orElse(null);
            if (pred != null) {
                LocalDate predEnd = getMilestoneEndDate(pred);
                earliestPredecessorEnd.merge(succId, predEnd.toEpochDay(), Math::max);
            }
        }

        LocalDate cursor = LocalDate.now().plusDays(1);
        if (project.getStartDate() != null) {
            LocalDate projectStart = toLocalDate(project.getStartDate());
            if (projectStart.isAfter(cursor)) cursor = projectStart;
        }

        List<ProjectScheduleResponse.MilestoneSchedule> schedules = new ArrayList<>();
        int weekNum = 1;
        LocalDate overallStart = null;
        LocalDate overallEnd = null;

        for (ProjectMilestone milestone : milestones) {
            ProjectScheduleResponse.MilestoneSchedule ms = new ProjectScheduleResponse.MilestoneSchedule();
            ms.setMilestoneId(milestone.getId());
            ms.setMilestoneTitle(milestone.getTitle());
            ms.setSortOrder(milestone.getSortOrder());

            Long predEndDay = earliestPredecessorEnd.get(milestone.getId());
            if (predEndDay != null) {
                LocalDate predEnd = LocalDate.ofEpochDay(predEndDay + 1);
                if (predEnd.isAfter(cursor)) cursor = predEnd;
            }

            long durationDays = estimateDurationDays(milestone);
            LocalDate weekStart = findNextWorkday(cursor);
            LocalDate weekEnd = weekStart.plusDays((long) durationDays - 1);

            ms.setSuggestedWeekStart(weekStart.toString());
            ms.setSuggestedWeekEnd(weekEnd.toString());
            ms.setWeekNumber(weekNum);

            List<ProjectScheduleResponse.ServiceSlot> slots = new ArrayList<>();
            boolean feasible = true;
            LocalDate slotCursor = weekStart;

            List<Service> milestoneServices = new ArrayList<>(milestone.getServices());
            if (milestoneServices.isEmpty()) {
                List<Service> projectServices = new ArrayList<>();
                if (project.getServices() != null) projectServices.addAll(project.getServices());
                milestoneServices = projectServices.stream().limit(1).toList();
            }

            for (Service svc : milestoneServices) {
                ProjectScheduleResponse.ServiceSlot slot = new ProjectScheduleResponse.ServiceSlot();
                slot.setServiceId(svc.getId());
                slot.setServiceName(svc.getName());
                if (svc.getProvider() != null) {
                    slot.setProviderName(svc.getProvider().getName());
                }

                if (svc.getPricingType() == PricingType.FIXED) {
                    slot.setPrice(svc.getPrice());
                    slot.setDurationHours(0);
                    slot.setScore(0.8);

                    LocalDateTime suggestedDateTime = findNextWorkday(slotCursor).atTime(9, 0);
                    slot.setSuggestedDate(suggestedDateTime.toString());
                    slot.setSuggestedTime("Full day");

                    try {
                        availabilityService.validateBookingAvailability(
                                svc.getId(), suggestedDateTime, 1);
                        slot.setScore(0.9);
                    } catch (Exception e) {
                        feasible = false;
                        slot.setScore(0.3);
                    }

                    slots.add(slot);
                } else {
                    slot.setPrice(svc.getPrice());
                    double duration = 2.0;
                    slot.setDurationHours(duration);
                    slot.setPrice(svc.getPrice() != null ? svc.getPrice().multiply(BigDecimal.valueOf(duration)) : null);
                    slot.setScore(0.7);

                    LocalDateTime suggestedDateTime = slotCursor.atTime(9, 0);
                    slot.setSuggestedDate(suggestedDateTime.toString());
                    slot.setSuggestedTime("09:00");

                    try {
                        availabilityService.validateBookingAvailability(
                                svc.getId(), suggestedDateTime, duration);
                        slot.setScore(0.9);
                    } catch (Exception e) {
                        feasible = false;
                        slot.setScore(0.3);
                    }

                    slots.add(slot);
                    slotCursor = slotCursor.plusDays(1);
                }
            }

            ms.setServiceSlots(slots);
            ms.setHasAvailability(feasible);

            if (milestoneServices.isEmpty()) {
                ms.setNote("No services assigned — add services to this milestone or the project");
            } else if (!feasible) {
                ms.setNote("Limited provider availability in this window — consider adjusting dates");
            } else {
                ms.setNote("Providers available in this window");
            }

            schedules.add(ms);

            if (overallStart == null) overallStart = weekStart;
            overallEnd = weekEnd;
            cursor = weekEnd.plusDays(1);
            weekNum++;
        }

        ProjectScheduleResponse response = new ProjectScheduleResponse();
        response.setProjectId(projectId);
        response.setMilestones(schedules);
        if (overallStart != null) response.setOverallStartDate(overallStart.toString());
        if (overallEnd != null) response.setOverallEndDate(overallEnd.toString());
        response.setTotalWeeks(weekNum - 1);
        response.setFeasible(schedules.stream().allMatch(ProjectScheduleResponse.MilestoneSchedule::isHasAvailability));

        return response;
    }

    @Transactional
    public WorkflowExecutionResponse executeAutomatedWorkflow(Long projectId, Long userId) {
        Project project = ensureProjectExists(projectId);
        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);
        List<ProjectDependency> dependencies = dependencyRepository.findByProjectIdOrderByIdAsc(projectId);

        Map<Long, Long> earliestPredEnd = new HashMap<>();
        for (ProjectDependency dep : dependencies) {
            Long predId = dep.getPredecessorMilestone().getId();
            Long succId = dep.getSuccessorMilestone().getId();
            ProjectMilestone pred = milestones.stream()
                    .filter(m -> m.getId().equals(predId)).findFirst().orElse(null);
            if (pred != null) {
                LocalDate predEnd = getMilestoneEndDate(pred);
                earliestPredEnd.merge(succId, predEnd.toEpochDay(), Math::max);
            }
        }

        LocalDate cursor = LocalDate.now().plusDays(1);
        if (project.getStartDate() != null) {
            LocalDate ps = toLocalDate(project.getStartDate());
            if (ps.isAfter(cursor)) cursor = ps;
        }

        List<Booking> createdBookings = new ArrayList<>();
        int milestonesActivated = 0;
        List<String> warnings = new ArrayList<>();

        for (ProjectMilestone milestone : milestones) {
            if (milestone.getStatus() == ProjectMilestoneStatus.COMPLETED ||
                    milestone.getStatus() == ProjectMilestoneStatus.CANCELLED) {
                LocalDate end = getMilestoneEndDate(milestone);
                cursor = end.plusDays(1);
                continue;
            }

            if (hasCancelledPredecessor(projectId, milestone.getId())) {
                milestone.setStatus(ProjectMilestoneStatus.CANCELLED);
                milestoneRepository.save(milestone);
                warnings.add("Skipped cancelled milestone (cascading): " + milestone.getTitle());
                continue;
            }

            if (milestone.getStatus() == ProjectMilestoneStatus.BLOCKED) {
                warnings.add("Skipped blocked milestone: " + milestone.getTitle());
                continue;
            }

            Long predEndDay = earliestPredEnd.get(milestone.getId());
            if (predEndDay != null) {
                LocalDate predEnd = LocalDate.ofEpochDay(predEndDay + 1);
                if (predEnd.isAfter(cursor)) cursor = predEnd;
            }

            long durationDays = estimateDurationDays(milestone);
            LocalDate weekStart = findNextWorkday(cursor);

            List<Service> services = new ArrayList<>(milestone.getServices());
            if (services.isEmpty() && project.getServices() != null) {
                services = new ArrayList<>(project.getServices());
            }

            LocalDate slotCursor = weekStart;
            for (Service svc : services) {
                double duration = svc.getPricingType() == PricingType.HOURLY ? 2.0 : 1.0;
                LocalDateTime date = slotCursor.atTime(9, 0);

                Booking booking = new Booking();
                User user = new User();
                user.setId(userId);
                booking.setUser(user);
                booking.setService(svc);
                booking.setProvider(svc.getProvider());
                booking.setProject(project);
                booking.setDate(date);
                booking.setDuration(duration);
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setNotes("Auto-created by project workflow for milestone: " + milestone.getTitle());

                if (svc.getPrice() != null && duration > 0) {
                    BigDecimal price = svc.getPricingType() == PricingType.HOURLY
                            ? svc.getPrice().multiply(BigDecimal.valueOf(duration)).setScale(2, java.math.RoundingMode.HALF_UP)
                            : svc.getPrice();
                    booking.setTotalPrice(price);
                }

                try {
                    Booking saved = bookingRepository.save(booking);
                    createdBookings.add(saved);
                    milestone.getBookings().add(saved);
                } catch (Exception e) {
                    warnings.add("Failed to create booking for " + svc.getName() + " in milestone " + milestone.getTitle() + ": " + e.getMessage());
                }

                slotCursor = slotCursor.plusDays(1);
            }

            milestone.setStatus(ProjectMilestoneStatus.PLANNED);
            milestone.setPlannedStartDate(java.util.Date.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            milestone.setPlannedEndDate(java.util.Date.from(weekStart.plusDays((long) durationDays - 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            milestoneRepository.save(milestone);
            milestonesActivated++;

            cursor = weekStart.plusDays(durationDays);
        }

        WorkflowExecutionResponse response = new WorkflowExecutionResponse();
        response.setProjectId(projectId);
        response.setStatus("EXECUTED");
        response.setBookingsCreated(createdBookings.size());
        response.setMilestonesActivated(milestonesActivated);
        response.setWarnings(warnings);
        response.setCreatedBookings(createdBookings.stream().map(b -> {
            WorkflowExecutionResponse.BookingSummary bs = new WorkflowExecutionResponse.BookingSummary();
            bs.setBookingId(b.getId());
            bs.setServiceId(b.getService() != null ? b.getService().getId() : null);
            bs.setServiceName(b.getService() != null ? b.getService().getName() : null);
            bs.setProviderName(b.getProvider() != null ? b.getProvider().getName() : null);
            bs.setDate(b.getDate() != null ? b.getDate().toString() : null);
            bs.setDuration(b.getDuration());
            bs.setTotalPrice(b.getTotalPrice());
            ProjectMilestone parentMilestone = milestones.stream()
                    .filter(m -> m.getBookings().contains(b))
                    .findFirst().orElse(null);
            if (parentMilestone != null) {
                bs.setMilestoneId(parentMilestone.getId());
                bs.setMilestoneTitle(parentMilestone.getTitle());
            }
            return bs;
        }).toList());

        return response;
    }

    private int computeDepth(Long milestoneId, Map<Long, List<Long>> adjacency,
                             Map<Long, Integer> cache, Set<Long> visiting) {
        if (cache.containsKey(milestoneId)) return cache.get(milestoneId);
        if (visiting.contains(milestoneId)) return 0;
        visiting.add(milestoneId);

        List<Long> successors = adjacency.getOrDefault(milestoneId, List.of());
        int maxChildDepth = 0;
        for (Long succ : successors) {
            maxChildDepth = Math.max(maxChildDepth, computeDepth(succ, adjacency, cache, visiting));
        }

        int depth = maxChildDepth + 1;
        cache.put(milestoneId, depth);
        visiting.remove(milestoneId);
        return depth;
    }

    private long estimateDurationDays(ProjectMilestone milestone) {
        if (milestone.getPlannedStartDate() != null && milestone.getPlannedEndDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    toLocalDate(milestone.getPlannedStartDate()),
                    toLocalDate(milestone.getPlannedEndDate())) + 1;
            return Math.max(1, days);
        }
        int serviceCount = milestone.getServices() != null ? milestone.getServices().size() : 0;
        return Math.max(3, serviceCount * 2L);
    }

    private LocalDate findNextWorkday(LocalDate date) {
        LocalDate d = date;
        while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.plusDays(1);
        }
        return d;
    }

    private LocalDate getMilestoneEndDate(ProjectMilestone milestone) {
        if (milestone.getActualEndDate() != null) return toLocalDate(milestone.getActualEndDate());
        if (milestone.getPlannedEndDate() != null) return toLocalDate(milestone.getPlannedEndDate());
        return LocalDate.now().plusDays(3);
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    @Transactional
    public void updateServiceEstimatedHours(Long projectId, Long milestoneId, Long serviceId, double hours) {
        findMilestoneForProject(projectId, milestoneId);
        MilestoneService.MilestoneServiceId id = new MilestoneService.MilestoneServiceId();
        id.setMilestoneId(milestoneId);
        id.setServiceId(serviceId);
        milestoneServiceRepository.findById(id).ifPresent(ms -> {
            ms.setEstimatedHours(hours);
            milestoneServiceRepository.save(ms);
        });
    }

    @Transactional
    public WorkflowExecutionResponse allocateAndBook(Long projectId, Long userId) {
        Project project = ensureProjectExists(projectId);
        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);

        WorkflowExecutionResponse resp = new WorkflowExecutionResponse();
        resp.setProjectId(projectId);
        List<WorkflowExecutionResponse.BookingSummary> created = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        LocalDate cursor = LocalDate.now().plusDays(1);
        if (project.getStartDate() != null) {
            LocalDate ps = toLocalDate(project.getStartDate());
            if (ps.isAfter(cursor)) cursor = ps;
        }

        for (ProjectMilestone ms : milestones) {
            if (ms.getStatus() == ProjectMilestoneStatus.COMPLETED
                    || ms.getStatus() == ProjectMilestoneStatus.CANCELLED
                    || ms.getStatus() == ProjectMilestoneStatus.BLOCKED) {
                continue;
            }

            if (ms.getMilestoneServices().isEmpty()) {
                continue;
            }
            if (ms.getMilestoneServices().isEmpty()) {
                continue;
            }

            LocalDate weekStart = findNextWorkday(cursor);

            for (MilestoneService msSvc : ms.getMilestoneServices()) {
                Service svc = msSvc.getService();
                if (svc == null || svc.getProvider() == null) continue;

                boolean isFixedPrice = svc.getPricingType() == PricingType.FIXED;
                double hours = Math.max(0.5, msSvc.getEstimatedHours());
                boolean providerFavorsProjects = false;

                if (isFixedPrice) {
                    LocalDate slotCursor = weekStart;
                    while (slotCursor.getDayOfWeek() == DayOfWeek.SATURDAY || slotCursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        slotCursor = slotCursor.plusDays(1);
                    }

                    LocalDateTime slotStart = slotCursor.atTime(providerFavorsProjects ? 8 : 10, 0);

                    try {
                        availabilityService.validateBookingAvailability(svc.getId(), slotStart, hours);

                        Booking b = new Booking();
                        b.setDate(slotStart);
                        b.setDuration((int) Math.round(hours));
                        b.setService(svc);
                        b.setProvider(svc.getProvider());
                        User bUser = new User();
                        bUser.setId(userId);
                        b.setUser(bUser);
                        b.setProject(project);
                        b.setStatus(BookingStatus.CONFIRMED);
                        b.setTotalPrice(svc.getPrice() != null ? svc.getPrice() : java.math.BigDecimal.ZERO);
                        b.setNotes("Auto-allocated (fixed price) from project " + project.getTitle() + ", milestone: " + ms.getTitle());

                        bookingRepository.save(b);
                        ms.getBookings().add(b);

                        WorkflowExecutionResponse.BookingSummary cb = new WorkflowExecutionResponse.BookingSummary();
                        cb.setServiceName(svc.getName());
                        cb.setProviderName(svc.getProvider().getName());
                        cb.setDate(slotStart.toString());
                        cb.setDuration(hours);
                        cb.setTotalPrice(b.getTotalPrice());
                        cb.setMilestoneTitle(ms.getTitle());
                        created.add(cb);
                    } catch (Exception e) {
                        warnings.add("Could not book " + svc.getName() + ": " + e.getMessage());
                    }
                } else {
                    int slotsNeeded = (int) Math.ceil(hours);
                    int slotsBooked = 0;
                    int attempts = 0;
                    LocalDate slotCursor = weekStart;

                    while (slotsBooked < slotsNeeded && attempts < 30) {
                        attempts++;
                        if (slotCursor.getDayOfWeek() == DayOfWeek.SATURDAY || slotCursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
                            slotCursor = slotCursor.plusDays(1);
                            continue;
                        }

                        LocalDateTime slotStart = slotCursor.atTime(providerFavorsProjects ? 8 : 10, 0);
                        LocalDateTime slotEnd = slotStart.plusHours((long) Math.min(hours - slotsBooked, 8));

                        try {
                            availabilityService.validateBookingAvailability(svc.getId(), slotStart, slotEnd.getHour() - slotStart.getHour());

                            Booking b = new Booking();
                            b.setDate(slotStart);
                            b.setDuration(slotEnd.getHour() - slotStart.getHour());
                            b.setService(svc);
                            b.setProvider(svc.getProvider());
                            User bUser = new User();
                            bUser.setId(userId);
                            b.setUser(bUser);
                            b.setProject(project);
                            b.setStatus(BookingStatus.CONFIRMED);
                            b.setTotalPrice(svc.getPrice() != null ? svc.getPrice().multiply(java.math.BigDecimal.valueOf(b.getDuration())) : java.math.BigDecimal.ZERO);
                            b.setNotes("Auto-allocated from project " + project.getTitle() + ", milestone: " + ms.getTitle());

                            bookingRepository.save(b);
                            ms.getBookings().add(b);

                            WorkflowExecutionResponse.BookingSummary cb = new WorkflowExecutionResponse.BookingSummary();
                            cb.setServiceName(svc.getName());
                            cb.setProviderName(svc.getProvider().getName());
                            cb.setDate(slotStart.toString());
                            cb.setDuration((double) b.getDuration());
                            cb.setMilestoneTitle(ms.getTitle());
                            created.add(cb);
                            slotsBooked++;
                        } catch (Exception e) {
                            warnings.add("Could not book " + svc.getName() + " on " + slotStart.toLocalDate() + ": slot taken");
                        }
                        slotCursor = slotCursor.plusDays(1);
                    }

                    if (slotsBooked < slotsNeeded) {
                        warnings.add(svc.getName() + ": only " + slotsBooked + "/" + slotsNeeded + "h booked for milestone '" + ms.getTitle() + "'");
                    }
                }
            }

            if (ms.getStatus() == ProjectMilestoneStatus.PLANNED) {
                ms.setStatus(ProjectMilestoneStatus.IN_PROGRESS);
                milestoneRepository.save(ms);
            }

            long durationDays = estimateDurationDays(ms);
            cursor = weekStart.plusDays(durationDays);
        }

        resp.setCreatedBookings(created);
        resp.setWarnings(warnings);
        return resp;
    }
}
