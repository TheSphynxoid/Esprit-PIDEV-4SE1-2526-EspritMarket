package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Srv.Dto.*;
import net.thesphynx.espritmarket.Srv.Entity.*;
import net.thesphynx.espritmarket.Srv.Mapper.ProjectMilestoneMapper;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectDependencyRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectMilestoneRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ProjectAssistantService {

    private static final List<String> STANDARD_PHASES = List.of(
            "Discovery & Requirements", "Design & Planning", "Execution & Development",
            "Review & Validation", "Delivery & Handoff"
    );

    public record NamedWorkflowTemplate(String id, String name, String description, String icon, List<MilestoneTemplateResponse> milestones) {}

    private static final List<NamedWorkflowTemplate> WORKFLOW_TEMPLATES = List.of(
            new NamedWorkflowTemplate("website", "Website Development",
                    "End-to-end website build with design, frontend, backend, testing, and launch phases.",
                    "globe",
                    List.of(
                            buildTpl("Discovery & Requirements", 0, "Gather requirements, define scope, create wireframes"),
                            buildTpl("UI/UX Design", 1, "Design mockups, prototype, user flows"),
                            buildTpl("Frontend Development", 2, "Build responsive pages, components, routing"),
                            buildTpl("Backend & API Development", 3, "Server logic, database, integrations"),
                            buildTpl("Testing & QA", 4, "Unit tests, integration tests, bug fixes"),
                            buildTpl("Deployment & Launch", 5, "Deploy to production, DNS, SSL, monitoring")
                    )),
            new NamedWorkflowTemplate("mobile-app", "Mobile App Development",
                    "Complete mobile app lifecycle from research to app store deployment.",
                    "smartphone",
                    List.of(
                            buildTpl("Market Research & Planning", 0, "Competitive analysis, feature prioritization"),
                            buildTpl("UI/UX & Prototyping", 1, "Screens, navigation flow, interactive prototype"),
                            buildTpl("Frontend Development", 2, "Build native or cross-platform UI"),
                            buildTpl("Backend & API Integration", 3, "Server APIs, push notifications, auth"),
                            buildTpl("Testing & Bug Fixing", 4, "Device testing, performance, accessibility"),
                            buildTpl("App Store Submission", 5, "Prepare assets, metadata, submit for review")
                    )),
            new NamedWorkflowTemplate("brand-identity", "Brand Identity",
                    "Create a complete brand identity system with logo, colors, typography, and guidelines.",
                    "palette",
                    List.of(
                            buildTpl("Brand Strategy & Research", 0, "Target audience, competitors, positioning"),
                            buildTpl("Logo Design", 1, "Concept exploration, finalization, file formats"),
                            buildTpl("Color Palette & Typography", 2, "Primary/secondary colors, font selection"),
                            buildTpl("Brand Guidelines", 3, "Usage rules, tone of voice, dos and don'ts"),
                            buildTpl("Collateral Design", 4, "Business cards, letterhead, social templates")
                    )),
            new NamedWorkflowTemplate("content-production", "Content Production",
                    "Plan, create, and deliver a content campaign across multiple channels.",
                    "edit",
                    List.of(
                            buildTpl("Content Strategy", 0, "Audience research, content calendar, themes"),
                            buildTpl("Copywriting", 1, "Blog posts, social media, ad copy"),
                            buildTpl("Visual Content Creation", 2, "Graphics, photos, video clips"),
                            buildTpl("Review & Approval", 3, "Stakeholder review, revisions"),
                            buildTpl("Publishing & Distribution", 4, "Schedule posts, monitor engagement")
                    )),
            new NamedWorkflowTemplate("generic", "Generic Project",
                    "A flexible template for any project with standard phases.",
                    "layers",
                    List.of(
                            buildTpl("Kickoff & Planning", 0, "Define goals, scope, timeline, team roles"),
                            buildTpl("Design & Prototyping", 1, "Create designs, prototypes, or blueprints"),
                            buildTpl("Development & Execution", 2, "Build the deliverables"),
                            buildTpl("Review & Iteration", 3, "Internal review, revisions, quality check"),
                            buildTpl("Delivery & Handoff", 4, "Final delivery, documentation, training")
                    ))
    );

    public List<NamedWorkflowTemplate> listWorkflowTemplates() {
        return WORKFLOW_TEMPLATES;
    }

    public MilestoneTemplateResponse.TemplateSet applyNamedTemplate(String templateId) {
        NamedWorkflowTemplate tpl = WORKFLOW_TEMPLATES.stream()
                .filter(t -> t.id().equals(templateId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Workflow Template", Long.parseLong(templateId.hashCode() + "")));

        List<MilestoneTemplateResponse> milestones = new ArrayList<>(tpl.milestones());
        for (int i = 0; i < milestones.size(); i++) {
            milestones.set(i, buildTemplate(milestones.get(i).getTitle(), i,
                    milestones.get(i).getDetails()));
        }

        List<DependencySuggestion> deps = generateDependencySuggestionsForTemplates(milestones);

        MilestoneTemplateResponse.TemplateSet set = new MilestoneTemplateResponse.TemplateSet();
        set.setMilestones(milestones);
        set.setSuggestedDependencies(deps);
        return set;
    }

    private final IProjectRepository projectRepository;
    private final IProjectMilestoneRepository milestoneRepository;
    private final IProjectDependencyRepository dependencyRepository;
    private final IBookingRepository bookingRepository;

    private final IServiceRepository serviceRepository;
    private final ProjectMilestoneMapper milestoneMapper;
    private final SlotScoringService slotScoringService;
    public ProjectAssistantService(IProjectRepository projectRepository,
                                   IProjectMilestoneRepository milestoneRepository,
                                   IProjectDependencyRepository dependencyRepository,
                                   IBookingRepository bookingRepository,
                                   IServiceRepository serviceRepository,
                                   ProjectMilestoneMapper milestoneMapper,
                                   SlotScoringService slotScoringService) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.dependencyRepository = dependencyRepository;
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.milestoneMapper = milestoneMapper;
        this.slotScoringService = slotScoringService;
    }

    public MilestoneTemplateResponse.TemplateSet generateMilestoneTemplates(Long projectId) {
        Project project = ensureProjectExists(projectId);
        List<ProjectMilestone> existing = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);

        if (!existing.isEmpty()) {
            MilestoneTemplateResponse.TemplateSet set = new MilestoneTemplateResponse.TemplateSet();
            set.setMilestones(List.of());
            set.setSuggestedDependencies(List.of());
            return set;
        }

        List<MilestoneTemplateResponse> templates = new ArrayList<>();
        List<Service> services = new ArrayList<>(project.getServices());

        if (services.isEmpty()) {
            for (int i = 0; i < STANDARD_PHASES.size(); i++) {
                templates.add(buildTemplate(STANDARD_PHASES.get(i), i));
            }
        } else {
            int order = 0;
            templates.add(buildTemplate("Project Kickoff & Alignment", order++));
            for (Service svc : services) {
                templates.add(buildTemplate(
                        svc.getName() + " — Execution", order++,
                        "Service delivery phase for: " + svc.getName()));
                templates.add(buildTemplate(
                        svc.getName() + " — Review & Validation", order++,
                        "Quality review and acceptance for: " + svc.getName()));
            }
            templates.add(buildTemplate("Final Integration & Delivery", order));
        }

        List<DependencySuggestion> suggestions = generateDependencySuggestionsForTemplates(templates);

        MilestoneTemplateResponse.TemplateSet set = new MilestoneTemplateResponse.TemplateSet();
        set.setMilestones(templates);
        set.setSuggestedDependencies(suggestions);
        return set;
    }

    private enum PhaseCategory {
        DISCOVERY, DESIGN, CONTENT, DEVELOPMENT, TESTING, REVIEW, DEPLOY, HANDOFF, UNKNOWN
    }

    private static final Map<PhaseCategory, Set<String>> PHASE_KEYWORDS = Map.of(
            PhaseCategory.DISCOVERY, Set.of("discovery", "requirements", "research", "analysis", "audit", "assessment", "kickoff", "alignment", "scoping"),
            PhaseCategory.DESIGN, Set.of("design", "ux", "ui", "wireframe", "mockup", "prototype", "architecture", "planning", "blueprint"),
            PhaseCategory.CONTENT, Set.of("content", "copy", "media", "asset", "graphic", "branding", "seo"),
            PhaseCategory.DEVELOPMENT, Set.of("development", "execution", "build", "implement", "coding", "frontend", "backend", "api", "integration", "database", "configuration"),
            PhaseCategory.TESTING, Set.of("testing", "qa", "quality", "validation", "verify", "debug", "performance", "security", "penetration"),
            PhaseCategory.REVIEW, Set.of("review", "revision", "iteration", "feedback", "approval", "sign-off"),
            PhaseCategory.DEPLOY, Set.of("deploy", "launch", "release", "go-live", "migration", "installation", "setup"),
            PhaseCategory.HANDOFF, Set.of("handoff", "training", "documentation", "support", "maintenance", "warranty", "onboarding")
    );

    private static final Map<PhaseCategory, Integer> PHASE_ORDER = Map.ofEntries(
            Map.entry(PhaseCategory.DISCOVERY, 10),
            Map.entry(PhaseCategory.DESIGN, 20),
            Map.entry(PhaseCategory.CONTENT, 25),
            Map.entry(PhaseCategory.DEVELOPMENT, 30),
            Map.entry(PhaseCategory.TESTING, 40),
            Map.entry(PhaseCategory.REVIEW, 50),
            Map.entry(PhaseCategory.DEPLOY, 60),
            Map.entry(PhaseCategory.HANDOFF, 70),
            Map.entry(PhaseCategory.UNKNOWN, 35)
    );

    private PhaseCategory classifyMilestone(String title) {
        if (title == null) return PhaseCategory.UNKNOWN;
        String lower = title.toLowerCase();
        for (Map.Entry<PhaseCategory, Set<String>> entry : PHASE_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) return entry.getKey();
            }
        }
        return PhaseCategory.UNKNOWN;
    }

    private boolean hasPath(Map<Long, List<Long>> graph, Long start, Long end) {
        if (start.equals(end)) return true;
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            List<Long> neighbors = graph.getOrDefault(current, List.of());
            for (Long neighbor : neighbors) {
                if (neighbor.equals(end)) return true;
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    public List<DependencySuggestion> suggestDependencies(Long projectId) {
        ensureProjectExists(projectId);
        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);
        List<ProjectDependency> existing = dependencyRepository.findByProjectIdOrderByIdAsc(projectId);

        if (milestones.size() < 2) return List.of();

        Set<String> existingPairs = existing.stream()
                .map(d -> d.getPredecessorMilestone().getId() + "->" + d.getSuccessorMilestone().getId())
                .collect(Collectors.toSet());

        Map<Long, List<Long>> adjGraph = new HashMap<>();
        for (ProjectDependency dep : existing) {
            adjGraph.computeIfAbsent(dep.getPredecessorMilestone().getId(), k -> new ArrayList<>())
                    .add(dep.getSuccessorMilestone().getId());
        }

        Map<Long, PhaseCategory> categoryMap = new LinkedHashMap<>();
        for (ProjectMilestone m : milestones) {
            categoryMap.put(m.getId(), classifyMilestone(m.getTitle()));
        }

        Set<String> suggestedKeys = new LinkedHashSet<>();
        List<DependencySuggestion> allSuggestions = new ArrayList<>();

        for (int i = 0; i < milestones.size(); i++) {
            ProjectMilestone current = milestones.get(i);
            PhaseCategory currentCat = categoryMap.get(current.getId());
            int currentOrder = PHASE_ORDER.get(currentCat);
            boolean currentCompleted = current.getStatus() == ProjectMilestoneStatus.COMPLETED;

            for (int j = i + 1; j < milestones.size(); j++) {
                ProjectMilestone later = milestones.get(j);
                PhaseCategory laterCat = categoryMap.get(later.getId());
                int laterOrder = PHASE_ORDER.get(laterCat);

                String pairKey = current.getId() + "->" + later.getId();
                if (existingPairs.contains(pairKey)) continue;

                if (currentCompleted && later.getStatus() == ProjectMilestoneStatus.COMPLETED) continue;

                double confidence = 0.0;
                String reason = null;

                if (laterOrder > currentOrder + 10) {
                    if (currentCat == PhaseCategory.DISCOVERY) {
                        confidence = 0.80;
                        reason = phaseLabel(currentCat) + " must complete before " + phaseLabel(laterCat) + " can begin — foundational input required";
                    } else if (currentCat == PhaseCategory.DESIGN && (laterCat == PhaseCategory.DEVELOPMENT || laterCat == PhaseCategory.DEPLOY)) {
                        confidence = 0.85;
                        reason = phaseLabel(currentCat) + " deliverables are prerequisites for " + phaseLabel(laterCat);
                    } else if (currentCat == PhaseCategory.DEVELOPMENT && laterCat == PhaseCategory.TESTING) {
                        confidence = 0.90;
                        reason = "Code must be built before testing can start";
                    } else if (currentCat == PhaseCategory.DEVELOPMENT && laterCat == PhaseCategory.DEPLOY) {
                        confidence = 0.75;
                        reason = "Development must be ready before deployment";
                    } else if (currentCat == PhaseCategory.TESTING && (laterCat == PhaseCategory.DEPLOY || laterCat == PhaseCategory.HANDOFF)) {
                        confidence = 0.85;
                        reason = "Testing must pass before " + phaseLabel(laterCat).toLowerCase();
                    } else if (currentCat == PhaseCategory.REVIEW && laterCat == PhaseCategory.DEPLOY) {
                        confidence = 0.80;
                        reason = "Review approval needed before deployment";
                    } else {
                        confidence = 0.45;
                        reason = "Phase ordering suggests " + phaseLabel(currentCat).toLowerCase() + " precedes " + phaseLabel(laterCat).toLowerCase();
                    }
                } else if (currentOrder == laterOrder) {
                    if (currentCat == PhaseCategory.DEVELOPMENT && laterCat == PhaseCategory.DEVELOPMENT) {
                        if (current.getAssignedProviderId() != null && later.getAssignedProviderId() != null
                                && !current.getAssignedProviderId().equals(later.getAssignedProviderId())) {
                            confidence = 0.60;
                            reason = "Parallel work streams can proceed independently (different providers)";
                        } else {
                            continue;
                        }
                    } else if (current.getPlannedEndDate() != null && later.getPlannedStartDate() != null) {
                        long overlapDays = java.time.Duration.between(
                                toLocalDateTime(current.getPlannedStartDate()),
                                toLocalDateTime(later.getPlannedStartDate())).toDays();
                        if (overlapDays < 0) {
                            confidence = 0.65;
                            reason = "Schedule overlap detected — verify these can truly run in parallel";
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else if (laterOrder < currentOrder) {
                    confidence = 0.35;
                    reason = "Possible reverse dependency — " + later.getTitle() + " may need to feed into " + current.getTitle();
                }

                if (confidence >= 0.40) {
                    DependencySuggestion suggestion = new DependencySuggestion();
                    suggestion.setPredecessorMilestoneId(current.getId());
                    suggestion.setPredecessorMilestoneTitle(current.getTitle());
                    suggestion.setSuccessorMilestoneId(later.getId());
                    suggestion.setSuccessorMilestoneTitle(later.getTitle());
                    suggestion.setReason(reason);
                    suggestion.setConfidence(confidence);
                    // Temporarily store distance for sorting
                    suggestion.setDistance(j - i);
                    allSuggestions.add(suggestion);
                }
            }
        }

        // Sort by distance first (closest milestones), then by confidence
        allSuggestions.sort((a, b) -> {
            int distCmp = Integer.compare(a.getDistance(), b.getDistance());
            if (distCmp != 0) return distCmp;
            return Double.compare(b.getConfidence(), a.getConfidence());
        });

        List<DependencySuggestion> filteredSuggestions = new ArrayList<>();
        for (DependencySuggestion sug : allSuggestions) {
            if (!hasPath(adjGraph, sug.getPredecessorMilestoneId(), sug.getSuccessorMilestoneId())) {
                filteredSuggestions.add(sug);
                // Add to graph so we don't create transitive edges later
                adjGraph.computeIfAbsent(sug.getPredecessorMilestoneId(), k -> new ArrayList<>())
                        .add(sug.getSuccessorMilestoneId());
            }
        }

        filteredSuggestions.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        return filteredSuggestions.stream().limit(10).collect(Collectors.toList());
    }

    private String phaseLabel(PhaseCategory cat) {
        return switch (cat) {
            case DISCOVERY -> "Discovery";
            case DESIGN -> "Design";
            case CONTENT -> "Content";
            case DEVELOPMENT -> "Development";
            case TESTING -> "Testing";
            case REVIEW -> "Review";
            case DEPLOY -> "Deployment";
            case HANDOFF -> "Handoff";
            case UNKNOWN -> "This phase";
        };
    }

    public ProjectRiskAssessment assessRisks(Long projectId) {
        Project project = ensureProjectExists(projectId);
        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);
        List<ProjectDependency> dependencies = dependencyRepository.findByProjectIdOrderByIdAsc(projectId);

        ProjectRiskAssessment assessment = new ProjectRiskAssessment();
        assessment.setProjectId(projectId);

        List<ProjectRiskAssessment.RiskAlert> alerts = new ArrayList<>();

        Map<Long, Set<Long>> adjList = buildAdjacencyList(dependencies);
        List<List<Long>> criticalPath = computeCriticalPaths(milestones, adjList);

        Set<Long> criticalMilestoneIds = criticalPath.stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        Map<Long, ProjectMilestone> milestoneMap = milestones.stream()
                .collect(Collectors.toMap(ProjectMilestone::getId, m -> m));

        for (ProjectMilestone m : milestones) {
            if (m.getStatus() == ProjectMilestoneStatus.BLOCKED) {
                ProjectRiskAssessment.RiskAlert alert = new ProjectRiskAssessment.RiskAlert();
                alert.setSeverity("HIGH");
                alert.setType("BLOCKED_MILESTONE");
                alert.setMessage("Milestone is blocked and cannot proceed");
                alert.setMilestoneId(m.getId());
                alert.setMilestoneTitle(m.getTitle());
                alerts.add(alert);
            }

            if (m.getPlannedEndDate() != null && m.getStatus() != ProjectMilestoneStatus.COMPLETED
                    && m.getStatus() != ProjectMilestoneStatus.CANCELLED) {
                LocalDateTime plannedEnd = toLocalDateTime(m.getPlannedEndDate());
                if (plannedEnd.isBefore(LocalDateTime.now())) {
                    ProjectRiskAssessment.RiskAlert alert = new ProjectRiskAssessment.RiskAlert();
                    alert.setSeverity(criticalMilestoneIds.contains(m.getId()) ? "CRITICAL" : "HIGH");
                    alert.setType("SLIPPAGE");
                    alert.setMessage("Milestone planned end date has passed without completion");
                    alert.setMilestoneId(m.getId());
                    alert.setMilestoneTitle(m.getTitle());
                    alerts.add(alert);
                }
            }

            if (m.getActualStartDate() != null && m.getPlannedStartDate() != null) {
                LocalDateTime actual = toLocalDateTime(m.getActualStartDate());
                LocalDateTime planned = toLocalDateTime(m.getPlannedStartDate());
                long delayDays = java.time.Duration.between(planned, actual).toDays();
                if (delayDays > 3) {
                    ProjectRiskAssessment.RiskAlert alert = new ProjectRiskAssessment.RiskAlert();
                    alert.setSeverity("MEDIUM");
                    alert.setType("START_DELAY");
                    alert.setMessage("Milestone started " + delayDays + " days late");
                    alert.setMilestoneId(m.getId());
                    alert.setMilestoneTitle(m.getTitle());
                    alerts.add(alert);
                }
            }
        }

        for (ProjectMilestone m : milestones) {
            if (m.getStatus() == ProjectMilestoneStatus.PLANNED) {
                Set<Long> preds = adjList.getOrDefault(m.getId(), Collections.emptySet());
                boolean allPredsCompleted = true;
                for (Long predId : preds) {
                    ProjectMilestone pred = milestoneMap.get(predId);
                    if (pred != null && pred.getStatus() != ProjectMilestoneStatus.COMPLETED) {
                        allPredsCompleted = false;
                        break;
                    }
                }
                if (allPredsCompleted && preds.isEmpty()) {
                    ProjectRiskAssessment.RiskAlert alert = new ProjectRiskAssessment.RiskAlert();
                    alert.setSeverity("LOW");
                    alert.setType("READY_TO_START");
                    alert.setMessage("Milestone has no blocking dependencies and is ready to begin");
                    alert.setMilestoneId(m.getId());
                    alert.setMilestoneTitle(m.getTitle());
                    alerts.add(alert);
                }
            }
        }

        if (milestones.size() > 5 && dependencies.isEmpty()) {
            ProjectRiskAssessment.RiskAlert alert = new ProjectRiskAssessment.RiskAlert();
            alert.setSeverity("MEDIUM");
            alert.setType("NO_DEPENDENCIES");
            alert.setMessage("Project has " + milestones.size() + " milestones but no dependency links defined");
            alerts.add(alert);
        }

        Map<Long, Integer> depthMap = computeChainDepths(milestones, adjList);
        List<ProjectRiskAssessment.CriticalPathItem> pathItems = milestones.stream().map(m -> {
            ProjectRiskAssessment.CriticalPathItem item = new ProjectRiskAssessment.CriticalPathItem();
            item.setMilestoneId(m.getId());
            item.setMilestoneTitle(m.getTitle());
            item.setSortOrder(m.getSortOrder());
            item.setChainDepth(depthMap.getOrDefault(m.getId(), 0));
            item.setIsOnCriticalPath(criticalMilestoneIds.contains(m.getId()));
            return item;
        }).toList();

        assessment.setAlerts(alerts);
        assessment.setCriticalPath(pathItems);
        assessment.setOverallRiskScore(computeOverallRisk(alerts, milestones.size()));
        return assessment;
    }

    public ScheduleOptimizationResponse optimizeSchedule(Long projectId) {
        Project project = ensureProjectExists(projectId);
        List<Booking> bookings = bookingRepository.findAllByProjectId(projectId);
        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);

        ScheduleOptimizationResponse response = new ScheduleOptimizationResponse();
        response.setProjectId(projectId);

        List<ScheduleOptimizationResponse.ServiceScheduleRecommendation> recommendations = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        LocalDate projectStart = project.getStartDate() != null
                ? toLocalDate(project.getStartDate())
                : LocalDate.now();

        List<Booking> unscheduled = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING
                        || b.getStatus() == BookingStatus.PENDING_EVALUATION
                        || b.getStatus() == BookingStatus.TENTATIVE)
                .toList();

        if (unscheduled.isEmpty() && bookings.isEmpty()) {
            notes.add("No bookings linked to this project yet. Add services to get schedule recommendations.");
        }

        LocalDate rollingDate = projectStart;
        for (Booking booking : unscheduled) {
            Service svc = booking.getService();
            if (svc == null) continue;

            try {
                SlotSuggestionResponse suggestion = slotScoringService.suggestSlots(
                        svc.getId(), rollingDate, rollingDate.plusDays(14), projectId,
                        SlotScoringMode.PROJECT_FIRST, 3);

                ScheduleOptimizationResponse.ServiceScheduleRecommendation rec =
                        new ScheduleOptimizationResponse.ServiceScheduleRecommendation();
                rec.setServiceId(svc.getId());
                rec.setServiceName(svc.getName());
                rec.setBookingId(booking.getId());

                if (suggestion.getSuggestions() != null && !suggestion.getSuggestions().isEmpty()) {
                    ScoredTimeSlotDto best = suggestion.getSuggestions().get(0);
                    if (best.getSlot() != null) {
                        rec.setRecommendedDate(best.getSlot().getStart() != null
                                ? best.getSlot().getStart().toString() : null);
                    }
                    if (best.getScore() != null) {
                        rec.setScore(best.getScore().getFinalScore());
                        rec.setReasonCode(best.getScore().getReasonCode());
                    }
                    rec.setNotes(List.of("Top-scored slot from PROJECT_FIRST mode"));
                } else {
                    rec.setNotes(List.of("No available slots found in the search window"));
                    notes.add("No slots found for " + svc.getName() + " — consider widening the date range");
                }

                rec.setRecommendedDuration(booking.getDuration());
                recommendations.add(rec);

                rollingDate = rollingDate.plusDays(3);
            } catch (Exception e) {
                ScheduleOptimizationResponse.ServiceScheduleRecommendation rec =
                        new ScheduleOptimizationResponse.ServiceScheduleRecommendation();
                rec.setServiceId(svc.getId());
                rec.setServiceName(svc.getName());
                rec.setBookingId(booking.getId());
                rec.setNotes(List.of("Could not evaluate slots: " + e.getMessage()));
                recommendations.add(rec);
            }
        }

        if (milestones.size() > 0) {
            long incomplete = milestones.stream()
                    .filter(m -> m.getStatus() != ProjectMilestoneStatus.COMPLETED
                            && m.getStatus() != ProjectMilestoneStatus.CANCELLED)
                    .count();
            if (incomplete > 0) {
                notes.add(incomplete + " milestones still pending — align booking schedule with milestone deadlines");
            }
        }

        response.setRecommendations(recommendations);
        response.setOptimizationNotes(notes);
        return response;
    }

    private List<DependencySuggestion> generateDependencySuggestionsForTemplates(
            List<MilestoneTemplateResponse> templates) {
        List<DependencySuggestion> suggestions = new ArrayList<>();
        for (int i = 1; i < templates.size(); i++) {
            DependencySuggestion s = new DependencySuggestion();
            s.setPredecessorMilestoneId((long) (i - 1));
            s.setPredecessorMilestoneTitle(templates.get(i - 1).getTitle());
            s.setSuccessorMilestoneId((long) i);
            s.setSuccessorMilestoneTitle(templates.get(i).getTitle());
            s.setReason("Sequential phase dependency");
            s.setConfidence(0.9);
            suggestions.add(s);
        }
        return suggestions;
    }

    private MilestoneTemplateResponse buildTemplate(String title, int sortOrder) {
        return buildTemplate(title, sortOrder, null);
    }

    private static MilestoneTemplateResponse buildTpl(String title, int sortOrder) {
        return buildTpl(title, sortOrder, null);
    }

    private static MilestoneTemplateResponse buildTpl(String title, int sortOrder, String details) {
        MilestoneTemplateResponse t = new MilestoneTemplateResponse();
        t.setTitle(title);
        t.setDetails(details);
        t.setSortOrder(sortOrder);
        return t;
    }

    private MilestoneTemplateResponse buildTemplate(String title, int sortOrder, String details) {
        MilestoneTemplateResponse t = new MilestoneTemplateResponse();
        t.setTitle(title);
        t.setDetails(details);
        t.setSortOrder(sortOrder);
        t.setCategory(sortOrder == 0 ? "KICKOFF"
                : sortOrder == STANDARD_PHASES.size() - 1 ? "DELIVERY"
                : "EXECUTION");
        return t;
    }

    private Map<Long, Set<Long>> buildAdjacencyList(List<ProjectDependency> dependencies) {
        Map<Long, Set<Long>> adj = new HashMap<>();
        for (ProjectDependency dep : dependencies) {
            adj.computeIfAbsent(dep.getSuccessorMilestone().getId(), k -> new HashSet<>())
                    .add(dep.getPredecessorMilestone().getId());
        }
        return adj;
    }

    private List<List<Long>> computeCriticalPaths(List<ProjectMilestone> milestones,
                                                   Map<Long, Set<Long>> adjList) {
        Map<Long, Integer> depths = computeChainDepths(milestones, adjList);
        int maxDepth = depths.values().stream().max(Integer::compareTo).orElse(0);

        if (maxDepth == 0) return List.of();

        List<List<Long>> paths = new ArrayList<>();
        for (int target = maxDepth; target >= maxDepth; target--) {
            for (ProjectMilestone m : milestones) {
                if (depths.getOrDefault(m.getId(), 0) == target) {
                    List<Long> path = new ArrayList<>();
                    tracePath(m.getId(), adjList, depths, path);
                    Collections.reverse(path);
                    paths.add(path);
                }
            }
        }
        return paths;
    }

    private void tracePath(Long milestoneId, Map<Long, Set<Long>> adjList,
                           Map<Long, Integer> depths, List<Long> path) {
        path.add(milestoneId);
        Set<Long> preds = adjList.getOrDefault(milestoneId, Collections.emptySet());
        Long deepest = null;
        int deepestDepth = -1;
        for (Long predId : preds) {
            int d = depths.getOrDefault(predId, 0);
            if (d > deepestDepth) {
                deepestDepth = d;
                deepest = predId;
            }
        }
        if (deepest != null) {
            tracePath(deepest, adjList, depths, path);
        }
    }

    private Map<Long, Integer> computeChainDepths(List<ProjectMilestone> milestones,
                                                   Map<Long, Set<Long>> adjList) {
        Map<Long, Integer> depths = new HashMap<>();
        for (ProjectMilestone m : milestones) {
            depths.put(m.getId(), 0);
        }

        boolean changed = true;
        int iterations = 0;
        while (changed && iterations < milestones.size() + 1) {
            changed = false;
            iterations++;
            for (ProjectMilestone m : milestones) {
                Set<Long> preds = adjList.getOrDefault(m.getId(), Collections.emptySet());
                for (Long predId : preds) {
                    int newDepth = depths.getOrDefault(predId, 0) + 1;
                    if (newDepth > depths.getOrDefault(m.getId(), 0)) {
                        depths.put(m.getId(), newDepth);
                        changed = true;
                    }
                }
            }
        }
        return depths;
    }

    private Double computeOverallRisk(List<ProjectRiskAssessment.RiskAlert> alerts, int totalMilestones) {
        if (alerts.isEmpty()) return 0.0;
        double score = 0.0;
        for (ProjectRiskAssessment.RiskAlert alert : alerts) {
            score += switch (alert.getSeverity()) {
                case "CRITICAL" -> 0.4;
                case "HIGH" -> 0.25;
                case "MEDIUM" -> 0.1;
                default -> 0.02;
            };
        }
        return Math.min(1.0, score);
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Project ensureProjectExists(Long projectId) {
        return projectRepository.findById(projectId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    public ProjectProgressReportResponse generateProgressReport(Long projectId) {
        Project project = ensureProjectExists(projectId);
        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);
        List<Booking> bookings = bookingRepository.findAllByProjectId(projectId);

        long total = milestones.size();
        long completed = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.COMPLETED).count();
        long inProgress = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.IN_PROGRESS).count();
        long planned = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.PLANNED).count();
        long blocked = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.BLOCKED).count();

        long overdue = 0;
        LocalDateTime now = LocalDateTime.now();
        for (ProjectMilestone m : milestones) {
            if (m.getPlannedEndDate() != null
                    && m.getStatus() != ProjectMilestoneStatus.COMPLETED
                    && m.getStatus() != ProjectMilestoneStatus.CANCELLED
                    && toLocalDateTime(m.getPlannedEndDate()).isBefore(now)) {
                overdue++;
            }
        }

        long completedBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long pendingBookings = bookings.stream().filter(b ->
                b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.IN_PROGRESS).count();

        double budgetTotal = project.getBudget() != null ? project.getBudget().doubleValue() : 0;
        double budgetUsed = bookings.stream()
                .filter(b -> b.getTotalPrice() != null)
                .mapToDouble(b -> b.getTotalPrice().doubleValue())
                .sum();

        List<ProjectProgressReportResponse.RiskAlert> alerts = new ArrayList<>();
        if (overdue > 0) {
            alerts.add(ProjectProgressReportResponse.RiskAlert.builder()
                    .severity("HIGH").type("OVERDUE")
                    .message(overdue + " milestone" + (overdue > 1 ? "s are" : " is") + " past the planned end date")
                    .build());
        }
        if (blocked > 0) {
            alerts.add(ProjectProgressReportResponse.RiskAlert.builder()
                    .severity("HIGH").type("BLOCKED")
                    .message(blocked + " milestone" + (blocked > 1 ? "s are" : " is") + " currently blocked")
                    .build());
        }
        if (total > 0 && completed == 0 && inProgress == 0) {
            alerts.add(ProjectProgressReportResponse.RiskAlert.builder()
                    .severity("MEDIUM").type("STALLED")
                    .message("No milestones have been started yet")
                    .build());
        }
        if (pendingBookings == 0 && total > 0 && completed < total) {
            alerts.add(ProjectProgressReportResponse.RiskAlert.builder()
                    .severity("LOW").type("NO_ACTIVE_BOOKINGS")
                    .message("No active bookings found — consider scheduling services")
                    .build());
        }

        String assessment;
        double pct = total > 0 ? (completed * 100.0 / total) : 0;
        if (pct >= 80) assessment = "On track — project is nearing completion.";
        else if (pct >= 50) assessment = "Good progress — project is past the halfway mark.";
        else if (pct >= 20) assessment = "In progress — keep milestones moving.";
        else if (pct > 0) assessment = "Early stages — focus on completing initial milestones.";
        else if (overdue > 0) assessment = "At risk — overdue milestones need immediate attention.";
        else assessment = "Not started — set up milestones and begin execution.";

        return ProjectProgressReportResponse.builder()
                .projectId(projectId)
                .title(project.getTitle())
                .status(project.getStatus() != null ? project.getStatus().name() : "UNKNOWN")
                .priority(project.getPriority())
                .totalMilestones((int) total)
                .completedMilestones((int) completed)
                .inProgressMilestones((int) inProgress)
                .plannedMilestones((int) planned)
                .blockedMilestones((int) blocked)
                .completionPercentage(Math.round(pct * 10.0) / 10.0)
                .overdueMilestones((int) overdue)
                .totalBookings(bookings.size())
                .completedBookings((int) completedBookings)
                .pendingBookings((int) pendingBookings)
                .budgetUsed(Math.round(budgetUsed * 100.0) / 100.0)
                .budgetTotal(budgetTotal)
                .budgetPercentage(budgetTotal > 0 ? Math.round(budgetUsed / budgetTotal * 1000.0) / 10.0 : 0)
                .riskAlerts(alerts)
                .overallAssessment(assessment)
                .build();
    }

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.ofEntries(
            Map.entry("web", List.of("website", "web", "frontend", "backend", "landing page", "portal", "ecommerce", "e-commerce", "shop", "blog", "cms", "webapp", "web app", "saas", "dashboard", "admin panel")),
            Map.entry("mobile", List.of("mobile", "app", "android", "ios", "iphone", "react native", "flutter", "kotlin", "swift", "cross-platform", "hybrid", "pwa", "progressive web")),
            Map.entry("design", List.of("design", "ui", "ux", "logo", "brand", "branding", "graphic", "mockup", "prototype", "wireframe", "figma", "illustration", "visual", "identity")),
            Map.entry("content", List.of("content", "writing", "copy", "blog", "article", "social media", "seo", "video", "photography", "marketing", "creative", "campaign")),
            Map.entry("consulting", List.of("consult", "audit", "analysis", "strategy", "plan", "advisory", "assessment", "research", "review", "optimization"))
    );

    private static final Map<String, List<String>> PHASE_PATTERNS;
    static {
        Map<String, List<String>> patterns = new LinkedHashMap<>();
        patterns.put("Discovery & Research", List.of("research", "discovery", "requirements", "analysis", "audit", "assessment", "planning", "strategy", "consult"));
        patterns.put("Design & Planning", List.of("design", "ui", "ux", "brand", "mockup", "prototype", "wireframe", "visual", "architecture", "planning"));
        patterns.put("Content & Media", List.of("content", "write", "copy", "media", "video", "photo", "graphic", "creative", "marketing"));
        patterns.put("Development & Implementation", List.of("develop", "build", "implement", "code", "program", "integrate", "setup", "configure", "custom", "feature"));
        patterns.put("Testing & Quality Assurance", List.of("test", "qa", "quality", "review", "validate", "verify", "debug", "bug", "performance"));
        patterns.put("Deployment & Launch", List.of("deploy", "launch", "release", "go-live", "migrate", "host", "server", "infrastructure", "monitor"));
        patterns.put("Training & Handoff", List.of("train", "handoff", "document", "tutorial", "onboard", "transfer", "guide"));
        PHASE_PATTERNS = patterns;
    }

    public ProjectDecompositionResponse decomposeProject(String description) {
        if (description == null || description.isBlank()) {
            return ProjectDecompositionResponse.builder()
                    .categoryDetected("general")
                    .suggestedMilestoneCount(0)
                    .milestones(List.of())
                    .totalServicesMatched(0)
                    .build();
        }

        String descLower = description.toLowerCase();
        Set<String> descWords = new HashSet<>(Arrays.asList(descLower.split("[\\s,;.!?()]+")));
        descWords.removeIf(w -> w.length() < 3);

        String detectedCategory = detectCategory(descLower);
        List<Service> allServices = serviceRepository.findAllActive(
                org.springframework.data.domain.PageRequest.of(0, 200)
        ).getContent();

        Map<String, List<Service>> phaseToServices = new LinkedHashMap<>();
        int totalMatched = 0;

        for (Map.Entry<String, List<String>> phase : PHASE_PATTERNS.entrySet()) {
            List<Service> matched = allServices.stream()
                    .filter(s -> {
                        if (s.getName() == null) return false;
                        String nameLower = s.getName().toLowerCase();
                        if (s.getCategory() != null && s.getCategory().name().toLowerCase().equals(detectedCategory)) {
                            return true;
                        }
                        for (String kw : phase.getValue()) {
                            if (nameLower.contains(kw)) return true;
                        }
                        for (String word : descWords) {
                            if (nameLower.contains(word) && word.length() > 3) return true;
                        }
                        return false;
                    })
                    .sorted(Comparator.comparingInt((Service s) -> scoreServiceRelevance(s, descWords)).reversed())
                    .limit(3)
                    .toList();

            if (!matched.isEmpty()) {
                phaseToServices.put(phase.getKey(), matched);
                totalMatched += matched.size();
            }
        }

        List<ProjectDecompositionResponse.DecomposedMilestone> milestones = new ArrayList<>();
        int order = 0;
        for (Map.Entry<String, List<Service>> entry : phaseToServices.entrySet()) {
            List<Service> services = entry.getValue();
            double maxPrice = services.stream()
                    .filter(s -> s.getPrice() != null)
                    .mapToDouble(s -> s.getPrice().doubleValue())
                    .max().orElse(0);
            int estimatedDays;
            String phaseTitle = entry.getKey().toLowerCase();
            if (phaseTitle.contains("discovery") || phaseTitle.contains("research")) estimatedDays = 5;
            else if (phaseTitle.contains("design") || phaseTitle.contains("planning")) estimatedDays = 7;
            else if (phaseTitle.contains("development") || phaseTitle.contains("implementation") || phaseTitle.contains("develop") || phaseTitle.contains("build")) estimatedDays = 14;
            else if (phaseTitle.contains("content") || phaseTitle.contains("media")) estimatedDays = 7;
            else if (phaseTitle.contains("test") || phaseTitle.contains("quality")) estimatedDays = 5;
            else if (phaseTitle.contains("deploy") || phaseTitle.contains("launch")) estimatedDays = 3;
            else if (phaseTitle.contains("training") || phaseTitle.contains("handoff")) estimatedDays = 3;
            else estimatedDays = 5;

            milestones.add(ProjectDecompositionResponse.DecomposedMilestone.builder()
                    .title(entry.getKey())
                    .details("AI-suggested phase based on project description")
                    .sortOrder(order)
                    .estimatedDays(estimatedDays)
                    .estimatedCost(BigDecimal.valueOf(maxPrice).setScale(2, RoundingMode.HALF_UP))
                    .services(services.stream()
                            .map(s -> ProjectDecompositionResponse.DecomposedService.builder()
                                    .serviceId(s.getId())
                                    .serviceName(s.getName())
                                    .category(s.getCategory() != null ? s.getCategory().name() : null)
                                    .price(s.getPrice())
                                    .relevanceScore(scoreServiceRelevance(s, descWords))
                                    .build())
                            .toList())
                    .build());
            order++;
        }

        BigDecimal totalBudget = milestones.stream()
                .map(m -> m.getEstimatedCost() != null ? m.getEstimatedCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalDays = milestones.stream().mapToInt(m -> m.getEstimatedDays()).sum();
        BigDecimal budgetWithBuffer = totalBudget.multiply(BigDecimal.valueOf(1.2)).setScale(2, RoundingMode.HALF_UP);

        return ProjectDecompositionResponse.builder()
                .categoryDetected(detectedCategory)
                .suggestedMilestoneCount(milestones.size())
                .milestones(milestones)
                .totalServicesMatched(totalMatched)
                .estimatedBudget(totalBudget)
                .estimatedDays(totalDays)
                .estimatedBudgetWithBuffer(budgetWithBuffer)
                .build();
    }

    private String detectCategory(String descLower) {
        Map<String, Integer> scores = new HashMap<>();
        for (Map.Entry<String, List<String>> cat : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String kw : cat.getValue()) {
                if (descLower.contains(kw)) score++;
            }
            scores.put(cat.getKey(), score);
        }
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse("general");
    }

    private int scoreServiceRelevance(Service service, Set<String> descWords) {
        if (service.getName() == null) return 0;
        String nameLower = service.getName().toLowerCase();
        int score = 0;
        for (String word : descWords) {
            if (nameLower.contains(word)) score += word.length();
        }
        if (service.getCategory() != null) {
            String catLower = service.getCategory().name().toLowerCase();
            for (String word : descWords) {
                if (catLower.contains(word)) score += word.length() / 2;
            }
        }
        return score;
    }

    @lombok.Builder
    @lombok.Data
    public static class ScopeChangeResult {
        private Long milestoneId;
        private String milestoneTitle;
        private double changeScore;
        private String severity;
        private List<String> changes;
    }

    public List<ScopeChangeResult> detectScopeChanges(Long projectId) {
        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);
        List<ScopeChangeResult> results = new ArrayList<>();

        for (ProjectMilestone ms : milestones) {
            if (ms.getOriginalTitle() == null && ms.getOriginalDetails() == null) continue;
            String origTitle = ms.getOriginalTitle() != null ? ms.getOriginalTitle() : ms.getTitle();
            String origDetails = ms.getOriginalDetails() != null ? ms.getOriginalDetails() : (ms.getDetails() != null ? ms.getDetails() : "");
            String curTitle = ms.getTitle() != null ? ms.getTitle() : "";
            String curDetails = ms.getDetails() != null ? ms.getDetails() : "";

            Set<String> origWords = tokenize(origTitle + " " + origDetails);
            Set<String> curWords = tokenize(curTitle + " " + curDetails);

            if (origWords.isEmpty() && curWords.isEmpty()) continue;

            Set<String> intersection = new HashSet<>(origWords);
            intersection.retainAll(curWords);
            Set<String> union = new HashSet<>(origWords);
            union.addAll(curWords);

            double jaccard = union.isEmpty() ? 1.0 : (double) intersection.size() / union.size();
            double changeScore = (1.0 - jaccard) * 100;

            List<String> changes = new ArrayList<>();
            if (!origTitle.equals(curTitle)) changes.add("Title changed: \"" + origTitle + "\" -> \"" + curTitle + "\"");
            if (origDetails.length() > 0 && curDetails.length() == 0) changes.add("Description removed");
            if (origDetails.length() == 0 && curDetails.length() > 0) changes.add("Description added (" + curDetails.length() + " chars)");
            if (origDetails.length() > 0 && curDetails.length() > 0 && !origDetails.equals(curDetails)) {
                int diff = Math.abs(curDetails.length() - origDetails.length());
                changes.add("Description modified (length " + origDetails.length() + " -> " + curDetails.length() + ", diff=" + diff + " chars)");
            }

            Set<String> removed = new HashSet<>(origWords);
            removed.removeAll(curWords);
            Set<String> added = new HashSet<>(curWords);
            added.removeAll(origWords);
            if (!removed.isEmpty()) changes.add("Removed concepts: " + String.join(", ", removed));
            if (!added.isEmpty()) changes.add("New concepts: " + String.join(", ", added));

            if (changeScore >= 20 && !changes.isEmpty()) {
                String severity = changeScore >= 70 ? "HIGH" : changeScore >= 40 ? "MEDIUM" : "LOW";
                results.add(ScopeChangeResult.builder()
                        .milestoneId(ms.getId())
                        .milestoneTitle(ms.getTitle())
                        .changeScore(Math.round(changeScore * 10.0) / 10.0)
                        .severity(severity)
                        .changes(changes)
                        .build());
            }
        }

        return results;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        return Arrays.stream(text.toLowerCase().split("[\\s,;.!?()]+"))
                .filter(w -> w.length() > 2)
                .collect(java.util.stream.Collectors.toSet());
    }

    private int getPhaseTierByTitle(String title) {
        if (title.contains("Discovery")) return 1;
        if (title.contains("Design") || title.contains("Content")) return 2;
        if (title.contains("Development")) return 3;
        if (title.contains("Testing")) return 4;
        if (title.contains("Deployment")) return 5;
        if (title.contains("Training")) return 6;
        return 10; // Fallback
    }

    @org.springframework.transaction.annotation.Transactional
    public List<ProjectMilestoneResponse> applyDecomposition(Long projectId, String description) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        ProjectDecompositionResponse decomp = decomposeProject(description);

        List<ProjectMilestone> created = new ArrayList<>();
        for (ProjectDecompositionResponse.DecomposedMilestone ms : decomp.getMilestones()) {
            ProjectMilestone milestone = new ProjectMilestone();
            milestone.setProject(project);
            milestone.setTitle(ms.getTitle());
            milestone.setDetails(ms.getDetails());
            milestone.setStatus(ProjectMilestoneStatus.PLANNED);
            milestone.setSortOrder(ms.getSortOrder());
            milestone.setMilestoneType(MilestoneType.MILESTONE);
            milestone = milestoneRepository.save(milestone);
            created.add(milestone);
        }

        Map<Integer, List<ProjectMilestone>> tierToMilestones = new java.util.TreeMap<>();
        for (ProjectMilestone m : created) {
            int tier = getPhaseTierByTitle(m.getTitle());
            if (tier == 10) tier = m.getSortOrder() + 10; // Ensure uniqueness for fallbacks
            tierToMilestones.computeIfAbsent(tier, k -> new ArrayList<>()).add(m);
        }

        List<Integer> sortedTiers = new ArrayList<>(tierToMilestones.keySet());
        for (int i = 1; i < sortedTiers.size(); i++) {
            List<ProjectMilestone> currentTierMilestones = tierToMilestones.get(sortedTiers.get(i));
            List<ProjectMilestone> previousTierMilestones = tierToMilestones.get(sortedTiers.get(i - 1));

            for (ProjectMilestone current : currentTierMilestones) {
                for (ProjectMilestone previous : previousTierMilestones) {
                    ProjectDependency dep = new ProjectDependency();
                    dep.setProject(project);
                    dep.setPredecessorMilestone(previous);
                    dep.setSuccessorMilestone(current);
                    dependencyRepository.save(dep);
                }
            }
        }

        if (!created.isEmpty()) {
            LocalDate today = LocalDate.now();
            LocalDate date = today;
            for (ProjectMilestone m : created) {
                m.setPlannedStartDate(java.sql.Date.valueOf(date));
                int days = 5;
                String title = m.getTitle().toLowerCase();
                if (title.contains("design") || title.contains("planning")) days = 7;
                else if (title.contains("development") || title.contains("implementation") || title.contains("develop") || title.contains("build")) days = 14;
                else if (title.contains("content") || title.contains("media")) days = 7;
                else if (title.contains("test") || title.contains("quality")) days = 5;
                else if (title.contains("deploy") || title.contains("launch")) days = 3;
                date = date.plusDays(days);
                m.setPlannedEndDate(java.sql.Date.valueOf(date));
                milestoneRepository.save(m);
            }
        }

        return created.stream().map(milestoneMapper::toResponse).toList();
    }
}
