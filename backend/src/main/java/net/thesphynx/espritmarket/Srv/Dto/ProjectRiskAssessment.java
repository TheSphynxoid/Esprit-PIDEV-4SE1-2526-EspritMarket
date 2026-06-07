package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ProjectRiskAssessment {
    private Long projectId;
    private List<RiskAlert> alerts;
    private List<CriticalPathItem> criticalPath;
    private Double overallRiskScore;

    @Data
    public static class RiskAlert {
        private String severity;
        private String type;
        private String message;
        private Long milestoneId;
        private String milestoneTitle;
    }

    @Data
    public static class CriticalPathItem {
        private Long milestoneId;
        private String milestoneTitle;
        private Integer sortOrder;
        private Integer chainDepth;
        private Boolean isOnCriticalPath;
    }
}
