package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ServiceRiskAnalysisResponse {
    private Long projectId;
    private List<ServiceRiskItem> services;

    @Data
    public static class ServiceRiskItem {
        private Long serviceId;
        private String serviceName;
        private String category;
        private String providerName;
        private Long providerId;
        private String milestoneTitle;
        private Long milestoneId;
        private Integer riskLevel;
        private double completionProbability;
        private String confidence;
        private String recommendation;
        private List<String> keyFactors;
    }
}
