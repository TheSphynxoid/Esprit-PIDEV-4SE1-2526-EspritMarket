package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProjectDecompositionResponse {
    private String categoryDetected;
    private int suggestedMilestoneCount;
    private List<DecomposedMilestone> milestones;
    private int totalServicesMatched;
    private BigDecimal estimatedBudget;
    private int estimatedDays;
    private BigDecimal estimatedBudgetWithBuffer;

    @Data
    @Builder
    public static class DecomposedMilestone {
        private String title;
        private String details;
        private int sortOrder;
        private int estimatedDays;
        private BigDecimal estimatedCost;
        private List<DecomposedService> services;
    }

    @Data
    @Builder
    public static class DecomposedService {
        private Long serviceId;
        private String serviceName;
        private String category;
        private BigDecimal price;
        private double relevanceScore;
    }
}
