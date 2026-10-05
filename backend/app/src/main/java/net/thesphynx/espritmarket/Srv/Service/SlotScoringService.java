package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Srv.Dto.*;
import net.thesphynx.espritmarket.Srv.Entity.Project;
import net.thesphynx.espritmarket.Srv.Entity.ProjectMilestone;
import net.thesphynx.espritmarket.Srv.Entity.ProjectMilestoneStatus;
import net.thesphynx.espritmarket.Srv.Repository.IProjectMilestoneRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SlotScoringService {

    private static final double W_AVAILABILITY = 0.35;
    private static final double W_SCARCITY = 0.20;
    private static final double W_PROJECT_URGENCY = 0.15;
    private static final double W_PROJECT_PROGRESS = 0.10;
    private static final double W_RELIABILITY = 0.10;
    private static final double W_FAIRNESS = 0.05;
    private static final double W_TIE_BREAKER = 0.05;

    private final AvailabilityService availabilityService;
    private final IServiceRepository serviceRepository;
    private final IProjectRepository projectRepository;
    private final IProjectMilestoneRepository milestoneRepository;
    private final ProviderScoringService providerScoringService;

    public SlotScoringService(AvailabilityService availabilityService,
                              IServiceRepository serviceRepository,
                              IProjectRepository projectRepository,
                              IProjectMilestoneRepository milestoneRepository,
                              ProviderScoringService providerScoringService) {
        this.availabilityService = availabilityService;
        this.serviceRepository = serviceRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.providerScoringService = providerScoringService;
    }

    public SlotSuggestionResponse suggestSlots(Long serviceId,
                                               LocalDate startDate,
                                               LocalDate endDate,
                                               Long projectId,
                                               SlotScoringMode mode,
                                               int limit) {
        serviceRepository.findById(serviceId)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));

        SlotScoringMode effectiveMode = mode != null ? mode : SlotScoringMode.PROJECT_FIRST;
        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<TimeSlotDto> slots = availabilityService.getAvailableSlots(serviceId, startDate, endDate);
        Project project = resolveProject(projectId);

        net.thesphynx.espritmarket.Srv.Entity.Service service = serviceRepository.findById(serviceId)
                .filter(s -> s.getDeletedAt() == null)
                .orElse(null);
        Long providerId = (service != null && service.getProvider() != null) ? service.getProvider().getId() : null;

        List<ScoredTimeSlotDto> scored = new ArrayList<>();
        for (TimeSlotDto slot : slots) {
            ScoredTimeSlotDto scoredSlot = new ScoredTimeSlotDto();
            scoredSlot.setSlot(slot);
            scoredSlot.setScore(computeScore(slot, project, providerId, effectiveMode));
            scored.add(scoredSlot);
        }

        scored.sort(Comparator.comparing((ScoredTimeSlotDto s) -> s.getScore().getFinalScore()).reversed());

        List<ScoredTimeSlotDto> top = scored.stream().limit(safeLimit).toList();
        for (int i = 0; i < top.size(); i++) {
            top.get(i).setRank(i + 1);
        }

        SlotSuggestionResponse response = new SlotSuggestionResponse();
        response.setServiceId(serviceId);
        response.setProjectId(projectId);
        response.setMode(effectiveMode);
        response.setSuggestions(top);
        return response;
    }

    private SlotScoreBreakdownDto computeScore(TimeSlotDto slot, Project project, Long providerId, SlotScoringMode mode) {
        double availabilityWeight = slot.isAvailable() ? 1.0 : 0.0;
        double scarcityWeight = slot.getMaxConcurrent() <= 0
                ? 0.0
                : ((double) slot.getAvailableCapacity() / slot.getMaxConcurrent());

        double projectUrgencyWeight = 0.5;
        double projectProgressWeight = 0.5;

        double reliabilityWeight = providerId != null ? providerScoringService.computeReliability(providerId) : 0.5;
        double fairnessWeight = providerId != null ? providerScoringService.computeFairness(providerId) : 0.5;

        if (project != null) {
            projectUrgencyWeight = computeProjectUrgency(project);
            projectProgressWeight = computeProjectProgressWeight(project);
        }

        double tieBreakerWeight = computeTieBreakerWeight(slot, mode);

        double modeMultiplier = mode == SlotScoringMode.PROJECT_FIRST ? 1.12 : 1.0;

        double base = (availabilityWeight * W_AVAILABILITY)
                + (scarcityWeight * W_SCARCITY)
                + (projectUrgencyWeight * W_PROJECT_URGENCY)
                + (projectProgressWeight * W_PROJECT_PROGRESS)
                + (reliabilityWeight * W_RELIABILITY)
                + (fairnessWeight * W_FAIRNESS)
                + (tieBreakerWeight * W_TIE_BREAKER);

        double finalScore = Math.max(0.0, Math.min(1.0, base * modeMultiplier));

        SlotScoreBreakdownDto breakdown = new SlotScoreBreakdownDto();
        breakdown.setPolicyProfile(mode.name());
        breakdown.setAvailabilityWeight(round(availabilityWeight));
        breakdown.setScarcityWeight(round(scarcityWeight));
        breakdown.setProjectUrgencyWeight(round(projectUrgencyWeight));
        breakdown.setProjectProgressWeight(round(projectProgressWeight));
        breakdown.setReliabilityWeight(round(reliabilityWeight));
        breakdown.setFairnessWeight(round(fairnessWeight));
        breakdown.setTieBreakerWeight(round(tieBreakerWeight));
        breakdown.setModeMultiplier(round(modeMultiplier));
        breakdown.setFinalScore(round(finalScore));
        breakdown.setReasonCode(resolveReasonCode(finalScore, mode));
        return breakdown;
    }

    private Project resolveProject(Long projectId) {
        if (projectId == null) return null;
        return projectRepository.findById(projectId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    private double computeProjectUrgency(Project project) {
        if (project.getEstimatedEndDate() == null) return 0.5;
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(),
                project.getEstimatedEndDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        if (days <= 3) return 1.0;
        if (days <= 7) return 0.8;
        if (days <= 14) return 0.65;
        return 0.45;
    }

    private double computeProjectProgressWeight(Project project) {
        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(project.getId());
        if (milestones.isEmpty()) return 0.5;

        long completed = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.COMPLETED).count();
        double completion = (double) completed / milestones.size();

        return 1.0 - completion;
    }

    private double computeTieBreakerWeight(TimeSlotDto slot, SlotScoringMode mode) {
        if (slot.getStart() == null) {
            return 0.5;
        }

        LocalDateTime start = slot.getStart();
        int hour = start.getHour();
        int dayOfWeek = start.getDayOfWeek().getValue();
        double workdayBonus = (dayOfWeek <= 5) ? 0.1 : 0.0;

        if (mode == SlotScoringMode.COMPETITIVE) {
            if (hour < 10) return 1.0 + workdayBonus;
            if (hour < 14) return 0.8 + workdayBonus;
            if (hour < 18) return 0.6 + workdayBonus;
            return 0.4 + workdayBonus;
        }

        if (hour >= 9 && hour <= 17) return 1.0 + workdayBonus;
        if (hour >= 8 && hour <= 19) return 0.8 + workdayBonus;
        return 0.5 + workdayBonus;
    }

    private String resolveReasonCode(double score, SlotScoringMode mode) {
        if (mode == SlotScoringMode.PROJECT_FIRST && score >= 0.8) return "HIGH_PRIORITY_MATCH";
        if (mode == SlotScoringMode.COMPETITIVE && score >= 0.7) return "COMPETITIVE_ADVANTAGE";
        if (score >= 0.6) return "GOOD_BALANCED_MATCH";
        if (score >= 0.4) return "MODERATE_MATCH";
        if (score >= 0.2) return "LOW_PRIORITY_MATCH";
        return "POOR_MATCH";
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
