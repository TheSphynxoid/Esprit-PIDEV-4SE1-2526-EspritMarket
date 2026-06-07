package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ProviderStandingResponse {
    private Long providerId;
    private String providerName;
    private long totalBookings;
    private long completedBookings;
    private long cancelledBookings;
    private long disputedBookings;
    private long activeBookings;
    private double completionRate;
    private double cancellationRate;
    private double disputeRate;
    private long totalReviews;
    private Double averageRating;
    private long activeServices;
    private double mlReliabilityScore;
    private String mlRiskLevel;
    private String mlConfidence;
    private List<String> mlKeyFactors;
    private String mlRecommendation;
    private long xp;
    private String level;
    private int levelNumber;
}
