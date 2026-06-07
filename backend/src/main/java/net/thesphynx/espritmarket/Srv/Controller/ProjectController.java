package net.thesphynx.espritmarket.Srv.Controller;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.*;
import net.thesphynx.espritmarket.Srv.Service.BookingService;
import net.thesphynx.espritmarket.Srv.Service.ChatbotService;
import net.thesphynx.espritmarket.Srv.Service.MlPredictionService;
import net.thesphynx.espritmarket.Srv.Service.ProjectAssistantService;
import net.thesphynx.espritmarket.Srv.Service.ProjectOrchestrationService;
import net.thesphynx.espritmarket.Srv.Service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/srv/projects")
@Tag(name = "Srv - Projects")
@PreAuthorize("isAuthenticated()")
public class ProjectController {
    private final ProjectService projectService;
    private final BookingService bookingService;
    private final ProjectOrchestrationService projectOrchestrationService;
    private final ProjectAssistantService projectAssistantService;
    private final MlPredictionService mlPredictionService;
    private final ChatbotService chatbotService;
    private final UserRepository userRepository;

    public ProjectController(ProjectService projectService,
                             BookingService bookingService,
                             ProjectOrchestrationService projectOrchestrationService,
                             ProjectAssistantService projectAssistantService,
                             MlPredictionService mlPredictionService,
                             ChatbotService chatbotService,
                             UserRepository userRepository) {
        this.projectService = projectService;
        this.bookingService = bookingService;
        this.projectOrchestrationService = projectOrchestrationService;
        this.projectAssistantService = projectAssistantService;
        this.mlPredictionService = mlPredictionService;
        this.chatbotService = chatbotService;
        this.userRepository = userRepository;
    }

    private Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }

    @GetMapping
    @Operation(summary = "List projects")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Projects retrieved")})
    public PageResponse<ProjectResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return projectService.getAll(page, size);
    }

    @GetMapping("/my")
    @Operation(summary = "Projects I participate in")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "My projects retrieved")})
    public PageResponse<ProjectResponse> getMyProjects(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return projectService.getParticipating(extractUserId(auth), page, size);
    }

    @GetMapping("/open-positions")
    @Operation(summary = "Active projects with open positions")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Open position projects retrieved")})
    public PageResponse<ProjectResponse> getOpenPositions(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return projectService.getOpenPositions(extractUserId(auth), page, size);
    }

    @GetMapping("/eligible-services")
    @Operation(summary = "Services eligible for project participation")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Eligible services retrieved")})
    public PageResponse<ServiceResponse> getEligibleServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return projectService.getEligibleServices(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Project found"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ProjectResponse> getById(@PathVariable Long id) {
        return projectService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Project created")})
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request, Authentication auth) {
        return projectService.create(request, extractUserId(auth));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Project updated"),
        @ApiResponse(responseCode = "404", description = "Project not found"),
        @ApiResponse(responseCode = "403", description = "Not the project creator")
    })
    public ResponseEntity<ProjectResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody ProjectRequest request,
                                                    Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            return ResponseEntity.status(403).build();
        }
        if (projectService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(projectService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update project status with transition validation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "403", description = "Not the project creator"),
        @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<ProjectResponse> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody ProjectStatusUpdateRequest request,
                                                         Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(projectService.updateStatus(id, request));
    }

    @PostMapping("/{id}/members/{userId}")
    @Operation(summary = "Add member to project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Member added")})
    public ResponseEntity<ProjectResponse> addMember(@PathVariable Long id,
                                                      @PathVariable Long userId,
                                                      Authentication auth) {
        Long callerId = extractUserId(auth);
        if (!projectService.isOwner(id, callerId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(projectService.addMember(id, userId));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove member from project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Member removed")})
    public ResponseEntity<ProjectResponse> removeMember(@PathVariable Long id,
                                                         @PathVariable Long userId,
                                                         Authentication auth) {
        Long callerId = extractUserId(auth);
        if (!projectService.isOwner(id, callerId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(projectService.removeMember(id, userId));
    }

    @PostMapping("/{id}/services/{serviceId}")
    @Operation(summary = "Link service to project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Service linked")})
    public ResponseEntity<ProjectResponse> addService(@PathVariable Long id,
                                                       @PathVariable Long serviceId,
                                                       Authentication auth) {
        Long callerId = extractUserId(auth);
        if (!projectService.isOwner(id, callerId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(projectService.addService(id, serviceId));
    }

    @DeleteMapping("/{id}/services/{serviceId}")
    @Operation(summary = "Unlink service from project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Service unlinked")})
    public ResponseEntity<ProjectResponse> removeService(@PathVariable Long id,
                                                          @PathVariable Long serviceId,
                                                          Authentication auth) {
        Long callerId = extractUserId(auth);
        if (!projectService.isOwner(id, callerId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(projectService.removeService(id, serviceId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project (soft)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project deleted"),
        @ApiResponse(responseCode = "404", description = "Project not found"),
        @ApiResponse(responseCode = "403", description = "Not the project creator")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            return ResponseEntity.status(403).build();
        }
        if (projectService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/bookings")
    @Operation(summary = "Get bookings for a project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Project bookings retrieved")})
    public PageResponse<BookingResponse> getProjectBookings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookingService.getByProjectId(id, page, size);
    }

    @GetMapping("/{id}/milestones")
    @Operation(summary = "List milestones for a project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Project milestones retrieved")})
    public java.util.List<ProjectMilestoneResponse> getMilestones(@PathVariable Long id) {
        return projectOrchestrationService.getMilestones(id);
    }

    @PostMapping("/{id}/milestones")
    @Operation(summary = "Create milestone for a project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Milestone created")})
    public ProjectMilestoneResponse createMilestone(@PathVariable Long id,
                                                     @Valid @RequestBody ProjectMilestoneRequest request,
                                                     Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can manage milestones");
        }
        return projectOrchestrationService.createMilestone(id, request);
    }

    @PutMapping("/{id}/milestones/{milestoneId}")
    @Operation(summary = "Update milestone for a project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Milestone updated")})
    public ProjectMilestoneResponse updateMilestone(@PathVariable Long id,
                                                    @PathVariable Long milestoneId,
                                                    @Valid @RequestBody ProjectMilestoneRequest request,
                                                    Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can manage milestones");
        }
        return projectOrchestrationService.updateMilestone(id, milestoneId, request);
    }

    @DeleteMapping("/{id}/milestones/{milestoneId}")
    @Operation(summary = "Delete milestone from a project")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Milestone deleted")})
    public ResponseEntity<Void> deleteMilestone(@PathVariable Long id,
                                                 @PathVariable Long milestoneId,
                                                 Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            return ResponseEntity.status(403).build();
        }
        projectOrchestrationService.deleteMilestone(id, milestoneId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/milestones/reorder")
    @Operation(summary = "Batch reorder milestones")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Milestones reordered")})
    public java.util.List<ProjectMilestoneResponse> reorderMilestones(
            @PathVariable Long id,
            @Valid @RequestBody MilestoneReorderRequest request,
            Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can reorder milestones");
        }
        return projectOrchestrationService.reorderMilestones(id, request.getOrderedMilestoneIds());
    }

    @PostMapping("/{id}/milestones/{milestoneId}/bookings/{bookingId}")
    @Operation(summary = "Link booking to milestone")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Booking linked")})
    public ProjectMilestoneResponse linkBooking(@PathVariable Long id,
                                                 @PathVariable Long milestoneId,
                                                 @PathVariable Long bookingId,
                                                 Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can link bookings");
        }
        return projectOrchestrationService.linkBooking(id, milestoneId, bookingId);
    }

    @DeleteMapping("/{id}/milestones/{milestoneId}/bookings/{bookingId}")
    @Operation(summary = "Unlink booking from milestone")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Booking unlinked")})
    public ProjectMilestoneResponse unlinkBooking(@PathVariable Long id,
                                                   @PathVariable Long milestoneId,
                                                   @PathVariable Long bookingId,
                                                   Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can unlink bookings");
        }
        return projectOrchestrationService.unlinkBooking(id, milestoneId, bookingId);
    }

    @GetMapping("/{id}/dependencies")
    @Operation(summary = "List dependencies for a project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Project dependencies retrieved")})
    public java.util.List<ProjectDependencyResponse> getDependencies(@PathVariable Long id) {
        return projectOrchestrationService.getDependencies(id);
    }

    @PostMapping("/{id}/dependencies")
    @Operation(summary = "Create dependency between project milestones")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Dependency created")})
    public ProjectDependencyResponse createDependency(@PathVariable Long id,
                                                      @Valid @RequestBody ProjectDependencyRequest request,
                                                      Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can manage dependencies");
        }
        return projectOrchestrationService.createDependency(id, request);
    }

    @DeleteMapping("/{id}/dependencies/{dependencyId}")
    @Operation(summary = "Delete dependency from a project")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Dependency deleted")})
    public ResponseEntity<Void> deleteDependency(@PathVariable Long id,
                                                  @PathVariable Long dependencyId,
                                                  Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            return ResponseEntity.status(403).build();
        }
        projectOrchestrationService.deleteDependency(id, dependencyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/dependencies/bulk")
    @Operation(summary = "Bulk apply dependency suggestions")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Dependencies created")})
    public java.util.List<ProjectDependencyResponse> bulkApplyDependencies(@PathVariable Long id,
                                                                            @RequestBody java.util.List<ProjectDependencyRequest> requests,
                                                                            Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can manage dependencies");
        }
        return projectOrchestrationService.bulkApplyDependencies(id, requests);
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Get project orchestration timeline")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Project timeline retrieved")})
    public ProjectTimelineResponse getTimeline(@PathVariable Long id) {
        return projectOrchestrationService.getTimeline(id);
    }

    @PostMapping("/{id}/workflow/execute")
    @Operation(summary = "Execute orchestration workflow graph")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Workflow executed")})
    public ProjectTimelineResponse executeWorkflow(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can execute workflow");
        }
        return projectOrchestrationService.executeWorkflow(id);
    }

    @PostMapping("/{id}/workflow/replan")
    @Operation(summary = "Replan project timeline from dependency graph")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Project replanned")})
    public ProjectTimelineResponse replanProject(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can replan");
        }
        return projectOrchestrationService.replanProject(id);
    }

    @GetMapping("/{id}/slot-suggestions")
    @Operation(summary = "Get project-owned slot suggestions for a service")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Project slot suggestions retrieved")})
    public SlotSuggestionResponse getProjectSlotSuggestions(
            @PathVariable Long id,
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "PROJECT_FIRST") SlotScoringMode mode,
            @RequestParam(defaultValue = "10") int limit) {
        if (endDate == null) {
            endDate = startDate;
        }
        return projectOrchestrationService.suggestSlotsForProject(id, serviceId, startDate, endDate, mode, limit);
    }

    @GetMapping("/{id}/assistant/templates")
    @Operation(summary = "Auto-generate milestone templates for a project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Templates generated")})
    public MilestoneTemplateResponse.TemplateSet getMilestoneTemplates(@PathVariable Long id) {
        return projectAssistantService.generateMilestoneTemplates(id);
    }

    @GetMapping("/assistant/workflow-templates")
    @Operation(summary = "List available workflow templates")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Templates listed")})
    public java.util.List<ProjectAssistantService.NamedWorkflowTemplate> listWorkflowTemplates() {
        return projectAssistantService.listWorkflowTemplates();
    }

    @GetMapping("/{id}/assistant/apply-template/{templateId}")
    @Operation(summary = "Apply a named workflow template to a project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Template applied")})
    public MilestoneTemplateResponse.TemplateSet applyWorkflowTemplate(@PathVariable Long id,
                                                                        @PathVariable String templateId) {
        return projectAssistantService.applyNamedTemplate(templateId);
    }

    @GetMapping("/{id}/assistant/dependency-suggestions")
    @Operation(summary = "Suggest dependencies between milestones")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Dependency suggestions retrieved")})
    public java.util.List<DependencySuggestion> getDependencySuggestions(@PathVariable Long id) {
        return projectAssistantService.suggestDependencies(id);
    }

    @GetMapping("/{id}/assistant/risk-assessment")
    @Operation(summary = "Assess project risks — critical path, slippage, blocked chains")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Risk assessment retrieved")})
    public ProjectRiskAssessment getRiskAssessment(@PathVariable Long id) {
        return projectAssistantService.assessRisks(id);
    }

    @GetMapping("/{id}/progress-report")
    @Operation(summary = "Generate automated project progress report")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Progress report generated")})
    public ProjectProgressReportResponse getProgressReport(@PathVariable Long id) {
        return projectAssistantService.generateProgressReport(id);
    }

    @GetMapping("/{id}/assistant/schedule-optimization")
    @Operation(summary = "Optimize multi-booking schedule across project services")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Schedule optimization retrieved")})
    public ScheduleOptimizationResponse getScheduleOptimization(@PathVariable Long id) {
        return projectAssistantService.optimizeSchedule(id);
    }

    @PostMapping("/assistant/decompose")
    @Operation(summary = "AI project decomposition - analyze description and suggest milestones")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Decomposition generated")})
    public ProjectDecompositionResponse decomposeProject(@RequestBody Map<String, String> body, Authentication auth) {
        extractUserId(auth);
        String description = body.getOrDefault("description", "");
        return projectAssistantService.decomposeProject(description);
    }

    @PostMapping("/{id}/assistant/decompose-apply")
    @Operation(summary = "Apply decomposition: create milestones with sequential dependencies")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Workflow created from decomposition")})
    public List<ProjectMilestoneResponse> applyDecomposition(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can manage milestones");
        }
        String description = body.getOrDefault("description", "");
        return projectAssistantService.applyDecomposition(id, description);
    }

    @GetMapping("/{id}/scope-changes")
    @Operation(summary = "Detect scope changes in project milestones")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Scope changes detected")})
    public java.util.List<?> detectScopeChanges(@PathVariable Long id) {
        return projectAssistantService.detectScopeChanges(id);
    }

    @GetMapping("/{id}/ml-delay-prediction")
    @Operation(summary = "Predict project delay risk using trained ML model")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Delay prediction retrieved")})
    public ProjectDelayPredictionResponse getDelayPrediction(@PathVariable Long id) {
        return mlPredictionService.predictProjectDelay(projectService.findEntityById(id));
    }

    @GetMapping("/{id}/service-risk-analysis")
    @Operation(summary = "Analyze ML risk per service across project milestones")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Service risk analysis")})
    public ServiceRiskAnalysisResponse getServiceRiskAnalysis(@PathVariable Long id) {
        return mlPredictionService.analyzeServiceRisks(projectService.findEntityById(id));
    }

    @PostMapping("/{id}/milestones/{milestoneId}/services/{serviceId}")
    @Operation(summary = "Link service to milestone")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Service linked to milestone")})
    public ProjectMilestoneResponse linkServiceToMilestone(@PathVariable Long id,
                                                            @PathVariable Long milestoneId,
                                                            @PathVariable Long serviceId,
                                                            Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can manage milestone services");
        }
        return projectOrchestrationService.linkService(id, milestoneId, serviceId);
    }

    @DeleteMapping("/{id}/milestones/{milestoneId}/services/{serviceId}")
    @Operation(summary = "Unlink service from milestone")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Service unlinked from milestone")})
    public ProjectMilestoneResponse unlinkServiceFromMilestone(@PathVariable Long id,
                                                                @PathVariable Long milestoneId,
                                                                @PathVariable Long serviceId,
                                                                Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can manage milestone services");
        }
        return projectOrchestrationService.unlinkService(id, milestoneId, serviceId);
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Generate estimated booking schedule for project milestones")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Schedule generated")})
    public ProjectScheduleResponse generateSchedule(@PathVariable Long id) {
        return projectOrchestrationService.generateSchedule(id);
    }

    @PostMapping("/{id}/workflow/auto-execute")
    @Operation(summary = "Execute automated workflow — auto-create bookings for all milestones and activate the plan")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Workflow executed")})
    public WorkflowExecutionResponse executeAutomatedWorkflow(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        if (!projectService.isOwner(id, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the project creator can execute the workflow");
        }
        return projectOrchestrationService.executeAutomatedWorkflow(id, userId);
    }

    @PutMapping("/{projectId}/milestones/{milestoneId}/services/{serviceId}/hours")
    @Operation(summary = "Set estimated hours for a service in a milestone")
    public void updateServiceEstimatedHours(@PathVariable Long projectId,
                                               @PathVariable Long milestoneId,
                                               @PathVariable Long serviceId,
                                               @RequestParam double hours) {
        projectOrchestrationService.updateServiceEstimatedHours(projectId, milestoneId, serviceId, hours);
    }

    @PostMapping("/{id}/allocate-and-book")
    @Operation(summary = "Smart allocate: create bookings based on estimated hours per service, balanced across provider schedule")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Bookings created")})
    public WorkflowExecutionResponse allocateAndBook(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        return projectOrchestrationService.allocateAndBook(id, userId);
    }

    @PostMapping("/{id}/chat")
    @Operation(summary = "Chat with AI project assistant")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Chatbot response")})
    public ChatbotResponse chat(@PathVariable Long id, @RequestBody ChatbotRequest request) {
        return chatbotService.chat(id, request.getMessage());
    }
}
