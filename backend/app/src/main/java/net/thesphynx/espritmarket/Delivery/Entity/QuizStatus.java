package net.thesphynx.espritmarket.Delivery.Entity;

public enum QuizStatus {
    PENDING("En attente"),
    ACCEPTED_QUIZ("Quiz accepté"),
    REJECTED("Rejeté");

    private final String description;

    QuizStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
