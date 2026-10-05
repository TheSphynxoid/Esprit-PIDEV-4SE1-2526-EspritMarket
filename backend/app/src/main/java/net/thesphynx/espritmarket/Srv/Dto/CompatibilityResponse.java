package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CompatibilityResponse {
    private Long providerId;
    private String providerName;
    private Long clientId;
    private double overallScore;
    private String matchLevel;
    private List<CompatibilityFactor> factors;

    @Data
    @Builder
    public static class CompatibilityFactor {
        private String name;
        private double score;
        private int weight;
        private String detail;
    }
}
