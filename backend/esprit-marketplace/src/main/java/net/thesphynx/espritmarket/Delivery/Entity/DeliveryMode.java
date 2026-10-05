package net.thesphynx.espritmarket.Delivery.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;

public enum DeliveryMode {
    LIVRAISON_A_DOMICILE("Livraison a domicile"),
    RETRAIT_A_ESPRIT("Retrait-a-esprit");

    private final String label;

    DeliveryMode(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static DeliveryMode fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = normalize(value);

        return switch (normalized) {
            case "LIVRAISON_A_DOMICILE" -> LIVRAISON_A_DOMICILE;
            case "RETRAIT_A_ESPRIT" -> RETRAIT_A_ESPRIT;
            default -> throw new IllegalArgumentException("Invalid delivery mode: " + value);
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
