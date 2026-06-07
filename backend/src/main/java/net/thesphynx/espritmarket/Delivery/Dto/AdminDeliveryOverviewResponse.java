package net.thesphynx.espritmarket.Delivery.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDeliveryOverviewResponse {
    private List<DateCount> ordersPerDay;
    private List<DateCount> deliveriesPerDay;
    private List<DateCount> interviewsPerDay;

    private Long totalOrders;
    private Long totalDeliveries;

    private Long pendingDeliveries;
    private Long acceptedDeliveries;
    private Long deliveredDeliveries;
    private Long refusedDeliveries;
    private Long cancelledDeliveries;

    private Long totalVehicles;

    private Long totalCouriers;
    private Long acceptedCouriers;
    private Long pendingCouriers;
    private Long refusedCouriers;

    private List<MonthCount> couriersPerMonth;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateCount {
        private String date; // YYYY-MM-DD
        private Long count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthCount {
        private String month; // YYYY-MM
        private Long count;
    }
}
