package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.PricingType;
import net.thesphynx.espritmarket.Srv.Entity.ServiceCategory;
import net.thesphynx.espritmarket.Srv.Entity.ServiceStatus;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceResponse {
    private Long id;
    private String name;
    private String description;
    private ServiceCategory category;
    private PricingType pricingType;
    private BigDecimal price;
    private ServiceStatus status;
    private BigDecimal rating;
    private String location;
    private String imageUrl;
    private boolean allowProjectParticipation;
    private Long providerId;
    private String providerName;
    private List<String> tags;
    private List<ServicePackageResponse> packages;
    private BigDecimal startingPrice;
}
