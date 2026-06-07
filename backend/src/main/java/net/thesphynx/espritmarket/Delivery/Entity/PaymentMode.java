package net.thesphynx.espritmarket.Delivery.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;

public enum PaymentMode {
    CHEQUE("Cheque"),
    ESPECE_A_LA_LIVRAISON("Paiement en espece a la livraison"),
    CARTE_BANCAIRE("Paiement par carte bancaire");

    private final String label;

    PaymentMode(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static PaymentMode fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = normalize(value);

        return switch (normalized) {
            case "CHEQUE" -> CHEQUE;
            case "PAIEMENT_EN_ESPECE_A_LA_LIVRAISON", "ESPECE_A_LA_LIVRAISON" -> ESPECE_A_LA_LIVRAISON;
            case "PAIEMENT_PAR_CARTE_BANCAIRE", "CARTE_BANCAIRE" -> CARTE_BANCAIRE;
            default -> throw new IllegalArgumentException("Invalid payment mode: " + value);
        };
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');
    }
}
