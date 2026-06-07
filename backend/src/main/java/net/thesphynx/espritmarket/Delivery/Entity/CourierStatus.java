package net.thesphynx.espritmarket.Delivery.Entity;

public enum CourierStatus {
    PENDING("En cours"),
    ACCEPTED("Accepté"),
    REFUSED("Refusé");

    private final String label;

    CourierStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
