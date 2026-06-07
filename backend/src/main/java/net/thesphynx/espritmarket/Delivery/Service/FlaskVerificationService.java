package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Delivery.Dto.VerificationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class FlaskVerificationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${flask.url:http://localhost:5000}")
    private String flaskUrl;

    /**
     * Compare le permis déjà stocké en DB (byte[]) avec un selfie envoyé en byte[].
     * On convertit les deux en base64 avant d'envoyer à Flask.
     */
    public VerificationResponse verifyPermitWithSelfie(byte[] permitImageBytes,
                                                       String permitContentType,
                                                       byte[] selfieBytes,
                                                       String courierId) {
        // Convertir les images en base64 (format attendu par Flask)
        String permitBase64 = "data:" + (permitContentType != null ? permitContentType : "image/jpeg")
                + ";base64," + Base64.getEncoder().encodeToString(permitImageBytes);

        String selfieBase64 = "data:image/jpeg;base64,"
                + Base64.getEncoder().encodeToString(selfieBytes);

        // Construire le body JSON
        Map<String, String> body = new HashMap<>();
        body.put("permit_image", permitBase64);
        body.put("selfie_image", selfieBase64);
        body.put("courier_id", courierId);

        // Appel Flask
        ResponseEntity<Map> response = restTemplate
                .postForEntity(flaskUrl + "/verify-courier", body, Map.class);

        Map<?, ?> data = response.getBody();
        if (data == null) {
            return VerificationResponse.builder()
                    .verdict("ERROR")
                    .reason("Flask n'a retourné aucune réponse")
                    .build();
        }

        Map<?, ?> faceMatch = (Map<?, ?>) data.get("face_match");
        Map<?, ?> permitInfo = (Map<?, ?>) data.get("permit_info");

        return VerificationResponse.builder()
                .verdict((String) data.get("verdict"))
                .reason((String) data.get("reason"))
                .similarityPercent(faceMatch != null
                        ? ((Number) faceMatch.get("similarity_percent")).doubleValue()
                        : 0.0)
                .faceVerified(faceMatch != null && Boolean.TRUE.equals(faceMatch.get("verified")))
                .permitName(permitInfo != null ? (String) permitInfo.get("name") : null)
                .permitNumber(permitInfo != null ? (String) permitInfo.get("permit_number") : null)
                .expiryDate(permitInfo != null ? (String) permitInfo.get("expiry_date") : null)
                .build();
    }
}
