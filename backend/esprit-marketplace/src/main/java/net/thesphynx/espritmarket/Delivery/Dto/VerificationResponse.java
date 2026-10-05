package net.thesphynx.espritmarket.Delivery.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerificationResponse {
    private String verdict;           // "APPROVED" ou "REJECTED"
    private String reason;            // explication humaine
    private double similarityPercent; // ex: 94.2
    private boolean faceVerified;     // true/false DeepFace
    private String permitName;        // nom extrait OCR
    private String permitNumber;      // numéro permis OCR
    private String expiryDate;        // date expiration OCR
}
