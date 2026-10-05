package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Builder;
import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.PricingType;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ServiceComparisonResponse {
    private Long serviceId;
    private String serviceName;
    private String category;
    private PricingType pricingType;
    private BigDecimal price;
    private BigDecimal startingPrice;
    private String description;
    private Long providerId;
    private String providerName;
    private Double averageRating;
    private Long totalReviews;
    private long totalBookings;
    private long completedBookings;
    private double completionRate;
    private String level;
    private int levelNumber;
    private double mlReliabilityScore;
    private String mlRiskLevel;
    private int packageCount;
    private List<String> tags;
    private Double compatibilityScore;
    private String compatibilityMatchLevel;
}
