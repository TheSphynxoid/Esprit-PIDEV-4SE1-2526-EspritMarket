package net.thesphynx.espritmarket.Delivery.Entity;

public enum CourierProfileStatus {
    INCOMPLETE("Profil incomplet"),
    COMPLETED("Profil complete");

    private final String label;

    CourierProfileStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}