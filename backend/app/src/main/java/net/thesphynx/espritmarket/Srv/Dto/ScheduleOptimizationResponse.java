package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ScheduleOptimizationResponse {
    private Long projectId;
    private List<ServiceScheduleRecommendation> recommendations;
    private List<String> optimizationNotes;

    @Data
    public static class ServiceScheduleRecommendation {
        private Long serviceId;
        private String serviceName;
        private Long bookingId;
        private String recommendedDate;
        private Double recommendedDuration;
        private Double score;
        private String reasonCode;
        private List<String> notes;
    }
}
