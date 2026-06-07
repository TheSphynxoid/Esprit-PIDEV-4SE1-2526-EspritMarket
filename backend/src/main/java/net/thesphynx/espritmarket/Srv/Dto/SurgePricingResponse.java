package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SurgePricingResponse {
    private List<CategoryDemand> categories;
    private List<ProviderDemand> providers;

    @Data
    @Builder
    public static class CategoryDemand {
        private String category;
        private int bookingsThisWeek;
        private int bookingsLastWeek;
        private double growthPercent;
        private double demandLevel;
        private double suggestedMultiplier;
        private String badge;
    }

    @Data
    @Builder
    public static class ProviderDemand {
        private Long providerId;
        private String providerName;
        private int activeBookings;
        private int completedLastWeek;
        private double workloadPercent;
        private double suggestedMultiplier;
        private String badge;
        private int estimatedWaitDays;
    }
}
