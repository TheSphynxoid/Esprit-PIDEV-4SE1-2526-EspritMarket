package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.ProjectRequest;
import net.thesphynx.espritmarket.Srv.Dto.ProjectResponse;
import net.thesphynx.espritmarket.Srv.Dto.ProjectStatusUpdateRequest;
import net.thesphynx.espritmarket.Srv.Dto.ServiceResponse;
import net.thesphynx.espritmarket.Srv.Entity.Project;
import net.thesphynx.espritmarket.Srv.Entity.ProjectMilestone;
import net.thesphynx.espritmarket.Srv.Entity.ProjectMilestoneStatus;
import net.thesphynx.espritmarket.Srv.Entity.ProjectStatus;
import net.thesphynx.espritmarket.Srv.Mapper.ProjectMapper;
import net.thesphynx.espritmarket.Srv.Mapper.ServiceMapper;
import net.thesphynx.espritmarket.Srv.Repository.IProjectMilestoneRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@org.springframework.stereotype.Service
public class ProjectService {
    private final IProjectRepository projectRepository;
    private final IProjectMilestoneRepository milestoneRepository;
    private final IServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final ServiceMapper serviceMapper;
    private final MlPredictionService mlPredictionService;

    private static final Map<ProjectStatus, Set<ProjectStatus>> VALID_TRANSITIONS = new EnumMap<>(ProjectStatus.class);

    static {
        VALID_TRANSITIONS.put(ProjectStatus.PLANNED, Set.of(ProjectStatus.IN_PROGRESS, ProjectStatus.CANCELLED));
        VALID_TRANSITIONS.put(ProjectStatus.IN_PROGRESS, Set.of(ProjectStatus.COMPLETED, ProjectStatus.CANCELLED, ProjectStatus.PLANNED));
        VALID_TRANSITIONS.put(ProjectStatus.COMPLETED, Set.of(ProjectStatus.IN_PROGRESS));
        VALID_TRANSITIONS.put(ProjectStatus.CANCELLED, Set.of(ProjectStatus.PLANNED));
    }

    public ProjectService(IProjectRepository projectRepository,
                          IProjectMilestoneRepository milestoneRepository,
                          IServiceRepository serviceRepository,
                          UserRepository userRepository,
                          ProjectMapper projectMapper,
                          ServiceMapper serviceMapper,
                          MlPredictionService mlPredictionService) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
        this.serviceMapper = serviceMapper;
        this.mlPredictionService = mlPredictionService;
    }

    public PageResponse<ProjectResponse> getAll(int page, int size) {
        Page<Project> result = projectRepository.findAllActive(PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public PageResponse<ProjectResponse> getParticipating(Long userId, int page, int size) {
        Page<Project> result = projectRepository.findParticipatingByUserId(userId, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public PageResponse<ProjectResponse> getOpenPositions(Long userId, int page, int size) {
        List<ProjectStatus> openStatuses = List.of(ProjectStatus.PLANNED, ProjectStatus.IN_PROGRESS);
        Page<Project> result = projectRepository.findOpenPositionsByUserId(userId, openStatuses, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public PageResponse<ServiceResponse> getEligibleServices(int page, int size) {
        Page<net.thesphynx.espritmarket.Srv.Entity.Service> result =
                serviceRepository.findProjectEligible(PageRequest.of(page, size));
        List<ServiceResponse> content = result.getContent().stream()
                .map(serviceMapper::toResponse)
                .toList();
        return PageResponse.of(content, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public Optional<ProjectResponse> getById(Long id) {
        return projectRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .map(projectMapper::toResponse);
    }

    public Project findEntityById(Long id) {
        return projectRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request, Long creatorId) {
        Project entity = projectMapper.toEntity(request);
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + creatorId));
        entity.setCreator(creator);
        entity.getMembers().add(creator);
        return projectMapper.toResponse(projectRepository.save(entity));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project existing = projectRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));

        validateTransition(existing.getStatus(), request.getStatus());

        existing.setTitle(request.getTitle());
        existing.setDetails(request.getDetails());
        existing.setStartDate(request.getStartDate());
        existing.setEstimatedEndDate(request.getEstimatedEndDate());
        existing.setEndDate(request.getEndDate());
        existing.setBudget(request.getBudget());
        existing.setStatus(request.getStatus());
        existing.setPriority(request.getPriority());

        return projectMapper.toResponse(projectRepository.save(existing));
    }

    @Transactional
    public ProjectResponse updateStatus(Long id, ProjectStatusUpdateRequest request) {
        Project project = projectRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));

        validateTransition(project.getStatus(), request.getStatus());

        if (request.getStatus() == ProjectStatus.COMPLETED) {
            project.setEndDate(new java.util.Date());
        }

        project.setStatus(request.getStatus());
        Project saved = projectRepository.save(project);

        if (saved.getStatus() != ProjectStatus.PLANNED) {
            mlPredictionService.recordProjectSnapshot(saved);
        }

        return projectMapper.toResponse(saved);
    }

    @Transactional
    public void autoTransitionIfAllMilestonesCompleted(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getDeletedAt() == null)
                .orElse(null);
        if (project == null || project.getStatus() != ProjectStatus.IN_PROGRESS) return;

        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);
        if (milestones.isEmpty()) return;

        boolean allCompleted = milestones.stream()
                .allMatch(m -> m.getStatus() == ProjectMilestoneStatus.COMPLETED);
        if (allCompleted) {
            project.setStatus(ProjectStatus.COMPLETED);
            project.setEndDate(new java.util.Date());
            Project saved = projectRepository.save(project);
            mlPredictionService.recordProjectSnapshot(saved);
        }
    }

    @Transactional
    public ProjectResponse addMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        project.getMembers().add(user);
        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse removeMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getCreator() != null && project.getCreator().getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot remove the project creator");
        }

        project.getMembers().removeIf(m -> m.getId().equals(userId));
        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse addService(Long projectId, Long serviceId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        net.thesphynx.espritmarket.Srv.Entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        project.getServices().add(service);
        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse removeService(Long projectId, Long serviceId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        project.getServices().removeIf(s -> s.getId().equals(serviceId));
        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long id) {
        projectRepository.findById(id).ifPresent(project -> {
            project.setDeletedAt(java.time.LocalDateTime.now());
            projectRepository.save(project);
        });
    }

    private PageResponse<ProjectResponse> toPageResponse(Page<Project> page) {
        List<ProjectResponse> content = page.getContent().stream()
                .map(projectMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public boolean isOwner(Long projectId, Long userId) {
        return projectRepository.findById(projectId)
                .filter(p -> p.getDeletedAt() == null)
                .map(p -> p.getCreator() != null && p.getCreator().getId().equals(userId))
                .orElse(false);
    }

    private void validateTransition(ProjectStatus current, ProjectStatus target) {
        Set<ProjectStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new IllegalStateException(
                    "Invalid project status transition: " + current + " -> " + target);
        }
    }
}
