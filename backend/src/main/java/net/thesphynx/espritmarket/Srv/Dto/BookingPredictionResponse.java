package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import java.util.List;

@Data
public class BookingPredictionResponse {
    private double completionProbability;
    private String riskLevel;
    private String confidence;
    private List<String> keyFactors;
    private String recommendation;
}
