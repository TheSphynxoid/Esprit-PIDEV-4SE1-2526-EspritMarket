package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProjectProgressReportResponse {
    private Long projectId;
    private String title;
    private String status;
    private String priority;

    private int totalMilestones;
    private int completedMilestones;
    private int inProgressMilestones;
    private int plannedMilestones;
    private int blockedMilestones;
    private double completionPercentage;

    private int overdueMilestones;
    private int totalBookings;
    private int completedBookings;
    private int pendingBookings;

    private double budgetUsed;
    private double budgetTotal;
    private double budgetPercentage;

    private List<RiskAlert> riskAlerts;
    private String overallAssessment;

    @Data
    @Builder
    public static class RiskAlert {
        private String severity;
        private String type;
        private String message;
        private String milestoneTitle;
    }
}
