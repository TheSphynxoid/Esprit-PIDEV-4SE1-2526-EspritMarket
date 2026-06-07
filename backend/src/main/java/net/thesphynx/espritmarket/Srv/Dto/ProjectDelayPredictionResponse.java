package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import java.util.List;

@Data
public class ProjectDelayPredictionResponse {
    private double onTimeProbability;
    private String delayRiskLevel;
    private Double estimatedDelayDays;
    private List<String> keyFactors;
    private String recommendation;
}
