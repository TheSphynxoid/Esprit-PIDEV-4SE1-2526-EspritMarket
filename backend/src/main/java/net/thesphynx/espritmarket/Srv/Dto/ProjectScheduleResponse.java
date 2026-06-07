package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProjectScheduleResponse {
    private Long projectId;
    private List<MilestoneSchedule> milestones;
    private String overallStartDate;
    private String overallEndDate;
    private int totalWeeks;
    private boolean feasible;

    @Data
    public static class MilestoneSchedule {
        private Long milestoneId;
        private String milestoneTitle;
        private int sortOrder;
        private String suggestedWeekStart;
        private String suggestedWeekEnd;
        private int weekNumber;
        private List<ServiceSlot> serviceSlots;
        private boolean hasAvailability;
        private String note;
    }

    @Data
    public static class ServiceSlot {
        private Long serviceId;
        private String serviceName;
        private String providerName;
        private BigDecimal price;
        private String suggestedDate;
        private String suggestedTime;
        private double durationHours;
        private double score;
    }
}
