package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Srv.Dto.ChatbotResponse;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Entity.Project;
import net.thesphynx.espritmarket.Srv.Entity.ProjectMilestone;
import net.thesphynx.espritmarket.Srv.Entity.ProjectMilestoneStatus;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectMilestoneRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final IProjectRepository projectRepository;
    private final IProjectMilestoneRepository milestoneRepository;
    private final IBookingRepository bookingRepository;

    private static final Map<String, List<Pattern>> INTENT_PATTERNS = Map.ofEntries(
            Map.entry("PROJECT_STATUS", List.of(
                    Pattern.compile("(?i)(project |overall )?status"),
                    Pattern.compile("(?i)how.*going"),
                    Pattern.compile("(?i)progress"),
                    Pattern.compile("(?i)summary")
            )),
            Map.entry("MILESTONE_INFO", List.of(
                    Pattern.compile("(?i)milestone"),
                    Pattern.compile("(?i)next.*step"),
                    Pattern.compile("(?i)current.*task"),
                    Pattern.compile("(?i)what.*working")
            )),
            Map.entry("RISK_ANALYSIS", List.of(
                    Pattern.compile("(?i)risk"),
                    Pattern.compile("(?i)delay"),
                    Pattern.compile("(?i)problem"),
                    Pattern.compile("(?i)issue"),
                    Pattern.compile("(?i)block")
            )),
            Map.entry("PROVIDER_INFO", List.of(
                    Pattern.compile("(?i)provider"),
                    Pattern.compile("(?i)who.*assigned"),
                    Pattern.compile("(?i)team")
            )),
            Map.entry("TIMELINE", List.of(
                    Pattern.compile("(?i)timeline"),
                    Pattern.compile("(?i)deadline"),
                    Pattern.compile("(?i)when.*finish"),
                    Pattern.compile("(?i)due date"),
                    Pattern.compile("(?i)schedule")
            )),
            Map.entry("SUGGESTIONS", List.of(
                    Pattern.compile("(?i)suggest"),
                    Pattern.compile("(?i)recommend"),
                    Pattern.compile("(?i)advice"),
                    Pattern.compile("(?i)what.*should"),
                    Pattern.compile("(?i)how.*improve")
            ))
    );

    public ChatbotService(IProjectRepository projectRepository,
                          IProjectMilestoneRepository milestoneRepository,
                          IBookingRepository bookingRepository) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.bookingRepository = bookingRepository;
    }

    public ChatbotResponse chat(Long projectId, String userMessage) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ChatbotResponse.builder()
                    .message("Project not found. Please check the project ID.")
                    .intent("ERROR")
                    .suggestions(List.of("Check your project URL", "Browse available projects"))
                    .build();
        }

        List<ProjectMilestone> milestones = milestoneRepository.findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(projectId);

        String intent = classifyIntent(userMessage);
        return switch (intent) {
            case "PROJECT_STATUS" -> handleProjectStatus(project, milestones);
            case "MILESTONE_INFO" -> handleMilestoneInfo(project, milestones);
            case "RISK_ANALYSIS" -> handleRiskAnalysis(milestones);
            case "PROVIDER_INFO" -> handleProviderInfo(milestones);
            case "TIMELINE" -> handleTimeline(milestones);
            case "SUGGESTIONS" -> handleSuggestions(project, milestones);
            default -> handleFallback(project);
        };
    }

    private String classifyIntent(String message) {
        if (message == null || message.isBlank()) return "FALLBACK";
        for (Map.Entry<String, List<Pattern>> entry : INTENT_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(message).find()) {
                    return entry.getKey();
                }
            }
        }
        return "FALLBACK";
    }

    private ChatbotResponse handleProjectStatus(Project project, List<ProjectMilestone> milestones) {
        long total = milestones.size();
        long completed = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.COMPLETED).count();
        long inProgress = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.IN_PROGRESS).count();
        long blocked = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.BLOCKED).count();

        double completionPct = total > 0 ? (completed * 100.0 / total) : 0;

        StringBuilder msg = new StringBuilder();
        msg.append(String.format("**%s** — Overall Status\n\n", project.getTitle()));
        msg.append(String.format("Progress: %.0f%% (%d/%d milestones)\n", completionPct, completed, total));

        if (inProgress > 0) {
            msg.append(String.format("In Progress: %d milestone(s)\n", inProgress));
        }
        if (blocked > 0) {
            msg.append(String.format("Blocked: %d milestone(s) — needs attention!\n", blocked));
        }
        if (completionPct >= 75) {
            msg.append("\nGreat progress! The project is nearing completion.");
        } else if (completionPct >= 50) {
            msg.append("\nGood progress. Keep the momentum going.");
        } else if (completionPct > 0) {
            msg.append("\nEarly stages. Focus on completing current milestones.");
        } else {
            msg.append("\nProject is just getting started. Begin with the first milestone.");
        }

        List<String> suggestions = new ArrayList<>();
        if (blocked > 0) suggestions.add("Review blocked milestones");
        if (inProgress == 0 && completed < total) suggestions.add("Start the next milestone");
        if (completionPct < 100) suggestions.add("View milestone details");

        return ChatbotResponse.builder().message(msg.toString()).intent("PROJECT_STATUS").suggestions(suggestions).build();
    }

    private ChatbotResponse handleMilestoneInfo(Project project, List<ProjectMilestone> milestones) {
        ProjectMilestone current = milestones.stream()
                .filter(m -> m.getStatus() == ProjectMilestoneStatus.IN_PROGRESS)
                .findFirst()
                .orElse(milestones.stream()
                        .filter(m -> m.getStatus() == ProjectMilestoneStatus.PLANNED)
                        .findFirst()
                        .orElse(null));

        if (current == null) {
            return ChatbotResponse.builder()
                    .message("All milestones have been completed or cancelled!")
                    .intent("MILESTONE_INFO")
                    .suggestions(List.of("View project summary"))
                    .build();
        }

        StringBuilder msg = new StringBuilder();
        msg.append(String.format("**Current: %s**\n", current.getTitle()));
        msg.append(String.format("Status: %s\n", current.getStatus()));
        if (current.getDetails() != null) {
            msg.append(String.format("Details: %s\n", current.getDetails()));
        }
        if (current.getAssignedProviderId() != null) {
            msg.append(String.format("Assigned Provider: #%d\n", current.getAssignedProviderId()));
        }

        long remaining = milestones.stream()
                .filter(m -> m.getStatus() == ProjectMilestoneStatus.PLANNED || m.getStatus() == ProjectMilestoneStatus.IN_PROGRESS)
                .count();

        msg.append(String.format("\n%d milestone(s) remaining after this one.", remaining - 1));

        return ChatbotResponse.builder()
                .message(msg.toString())
                .intent("MILESTONE_INFO")
                .suggestions(List.of("View full timeline", "Check risks"))
                .build();
    }

    private ChatbotResponse handleRiskAnalysis(List<ProjectMilestone> milestones) {
        long blocked = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.BLOCKED).count();
        long overdue = milestones.stream()
                .filter(m -> m.getStatus() != ProjectMilestoneStatus.COMPLETED
                        && m.getStatus() != ProjectMilestoneStatus.CANCELLED
                        && m.getPlannedEndDate() != null
                        && new java.util.Date().after(m.getPlannedEndDate()))
                .count();
        long inProgress = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.IN_PROGRESS).count();

        StringBuilder msg = new StringBuilder();
        msg.append("**Risk Analysis**\n\n");

        if (blocked > 0 || overdue > 0) {
            msg.append("⚠️ Risks detected:\n");
            if (blocked > 0) msg.append(String.format("- %d blocked milestone(s)\n", blocked));
            if (overdue > 0) msg.append(String.format("- %d overdue milestone(s)\n", overdue));
            msg.append("\nRecommendation: Review blocked milestones and consider adjusting deadlines.");
        } else if (inProgress > 0) {
            msg.append("✅ No immediate risks detected.\n");
            msg.append(String.format("%d milestone(s) are actively being worked on. Keep monitoring progress.", inProgress));
        } else {
            msg.append("ℹ️ No active risks. All milestones are in planning or completed.");
        }

        List<String> suggestions = new ArrayList<>();
        if (blocked > 0) suggestions.add("Unblock milestones");
        if (overdue > 0) suggestions.add("Extend deadlines");
        suggestions.add("View project timeline");

        return ChatbotResponse.builder().message(msg.toString()).intent("RISK_ANALYSIS").suggestions(suggestions).build();
    }

    private ChatbotResponse handleProviderInfo(List<ProjectMilestone> milestones) {
        Map<Long, Long> providerCounts = milestones.stream()
                .filter(m -> m.getAssignedProviderId() != null)
                .collect(Collectors.groupingBy(ProjectMilestone::getAssignedProviderId, Collectors.counting()));

        if (providerCounts.isEmpty()) {
            return ChatbotResponse.builder()
                    .message("No providers have been assigned to milestones yet. Assign providers to enable multi-provider collaboration.")
                    .intent("PROVIDER_INFO")
                    .suggestions(List.of("Assign providers to milestones"))
                    .build();
        }

        StringBuilder msg = new StringBuilder();
        msg.append(String.format("**Team Overview** — %d provider(s) assigned\n\n", providerCounts.size()));
        for (Map.Entry<Long, Long> entry : providerCounts.entrySet()) {
            msg.append(String.format("- Provider #%d: %d milestone(s)\n", entry.getKey(), entry.getValue()));
        }

        long unassigned = milestones.stream().filter(m -> m.getAssignedProviderId() == null).count();
        if (unassigned > 0) {
            msg.append(String.format("\n%d milestone(s) still need provider assignment.", unassigned));
        }

        return ChatbotResponse.builder()
                .message(msg.toString())
                .intent("PROVIDER_INFO")
                .suggestions(List.of("Assign providers", "View provider standing"))
                .build();
    }

    private ChatbotResponse handleTimeline(List<ProjectMilestone> milestones) {
        long total = milestones.size();
        long completed = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.COMPLETED).count();
        long remaining = total - completed;

        if (remaining == 0) {
            return ChatbotResponse.builder()
                    .message("All milestones are completed! The project is done.")
                    .intent("TIMELINE")
                    .suggestions(List.of("View project summary"))
                    .build();
        }

        long inProgress = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.IN_PROGRESS).count();

        StringBuilder msg = new StringBuilder();
        msg.append(String.format("**Timeline Overview**\n\n"));
        msg.append(String.format("Completed: %d/%d milestones\n", completed, total));
        msg.append(String.format("In Progress: %d\n", inProgress));
        msg.append(String.format("Remaining: %d\n", remaining));

        ProjectMilestone next = milestones.stream()
                .filter(m -> m.getStatus() == ProjectMilestoneStatus.PLANNED)
                .findFirst()
                .orElse(null);

        if (next != null && next.getPlannedStartDate() != null) {
            msg.append(String.format("\nNext milestone: **%s** (planned: %s)", next.getTitle(), next.getPlannedStartDate()));
            if (next.getPlannedEndDate() != null) {
                msg.append(String.format(" → %s", next.getPlannedEndDate()));
            }
        }

        return ChatbotResponse.builder()
                .message(msg.toString())
                .intent("TIMELINE")
                .suggestions(List.of("View milestone details", "Check risks"))
                .build();
    }

    private ChatbotResponse handleSuggestions(Project project, List<ProjectMilestone> milestones) {
        List<String> suggestions = new ArrayList<>();

        long blocked = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.BLOCKED).count();
        long inProgress = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.IN_PROGRESS).count();
        long planned = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.PLANNED).count();
        long completed = milestones.stream().filter(m -> m.getStatus() == ProjectMilestoneStatus.COMPLETED).count();
        long unassigned = milestones.stream().filter(m -> m.getAssignedProviderId() == null).count();

        if (blocked > 0) suggestions.add("Resolve blocked milestones to unblock the project");
        if (inProgress == 0 && planned > 0) suggestions.add("Start working on the next planned milestone");
        if (unassigned > 0) suggestions.add("Assign providers to all unassigned milestones for parallel work");
        if (completed > 0 && completed == milestones.size()) suggestions.add("Project is complete! Consider leaving a review");
        if (planned > 3) suggestions.add("Consider breaking down milestones into smaller tasks for faster delivery");
        if (suggestions.isEmpty()) suggestions.add("Everything looks on track! Keep up the good work.");

        StringBuilder msg = new StringBuilder();
        msg.append(String.format("**Suggestions for %s**\n\n", project.getTitle()));
        for (int i = 0; i < suggestions.size(); i++) {
            msg.append(String.format("%d. %s\n", i + 1, suggestions.get(i)));
        }

        return ChatbotResponse.builder()
                .message(msg.toString())
                .intent("SUGGESTIONS")
                .suggestions(suggestions)
                .build();
    }

    private ChatbotResponse handleFallback(Project project) {
        return ChatbotResponse.builder()
                .message(String.format("I can help you with project **%s**. Try asking about:\n- Project status\n- Current milestone\n- Risk analysis\n- Timeline\n- Team/providers\n- Suggestions", project.getTitle()))
                .intent("FALLBACK")
                .suggestions(List.of("Project status", "Current milestone", "Risk analysis", "Timeline", "Suggestions"))
                .build();
    }
}
