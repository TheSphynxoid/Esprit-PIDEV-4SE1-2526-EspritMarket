package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.util.List;

@Data
public class WorkflowExecutionResponse {
    private Long projectId;
    private String status;
    private int bookingsCreated;
    private int milestonesActivated;
    private List<BookingSummary> createdBookings;
    private List<String> warnings;

    @Data
    public static class BookingSummary {
        private Long bookingId;
        private Long milestoneId;
        private String milestoneTitle;
        private Long serviceId;
        private String serviceName;
        private String providerName;
        private String date;
        private double duration;
        private java.math.BigDecimal totalPrice;
    }
}
