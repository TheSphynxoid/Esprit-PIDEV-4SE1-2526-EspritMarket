package net.thesphynx.espritmarket.Partnership.Service;

import lombok.RequiredArgsConstructor;
import net.thesphynx.espritmarket.Partnership.Dto.AiPredictRequest;
import net.thesphynx.espritmarket.Partnership.Dto.AiPredictResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AiMatchingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String AI_SERVICE_URL = "http://localhost:8001/predict";

    public double getMatchingScore(AiPredictRequest request) {
        try {
            System.out.println("Calling AI Service with request: " + request);
            AiPredictResponse response = restTemplate.postForObject(AI_SERVICE_URL, request, AiPredictResponse.class);
            double score = response != null ? response.getMatching_score() : 0.0;
            System.out.println("AI Service returned score: " + score);
            return score;
        } catch (Exception e) {
            System.err.println("Failed to call AI service at " + AI_SERVICE_URL + ": " + e.getMessage());
            return 0.0;
        }
    }
}
