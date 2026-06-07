package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.MilestoneType;
import net.thesphynx.espritmarket.Srv.Entity.ProjectMilestoneStatus;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class ProjectMilestoneResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String details;
    private Date plannedStartDate;
    private Date plannedEndDate;
    private Date actualStartDate;
    private Date actualEndDate;
    private ProjectMilestoneStatus status;
    private MilestoneType milestoneType;
    private String conditionExpression;
    private Long assignedProviderId;
    private String assignedProviderName;
    private String handoffNotes;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Long> linkedBookingIds;
    private List<MilestoneServiceSummary> services;

    @Data
    public static class MilestoneServiceSummary {
        private Long id;
        private String name;
        private String category;
        private java.math.BigDecimal price;
        private String pricingType;
        private Long providerId;
        private String providerName;
    }
}
