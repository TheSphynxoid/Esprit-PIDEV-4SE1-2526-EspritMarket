package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.PricingType;
import net.thesphynx.espritmarket.Srv.Entity.ServiceCategory;
import net.thesphynx.espritmarket.Srv.Entity.ServiceStatus;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceUpsertRequest {
    @NotBlank(message = "Service name is required")
    @Size(min = 3, max = 100, message = "Service name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Service description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Service category is required")
    private ServiceCategory category;

    @NotNull(message = "Pricing type is required")
    private PricingType pricingType;

    @DecimalMin(value = "1.0", message = "Price must be at least 1.0")
    @DecimalMax(value = "100000.0", message = "Price must not exceed 100000.0")
    private BigDecimal price;

    @NotNull(message = "Service status is required")
    private ServiceStatus status;

    @DecimalMin(value = "0.0", message = "Rating must be between 0.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Rating must be between 0.0 and 5.0")
    private BigDecimal rating;

    @NotBlank(message = "Service location is required")
    @Size(min = 2, max = 200, message = "Location must be between 2 and 200 characters")
    private String location;

    private String imageUrl;

    @NotNull(message = "Provider ID is required")
    @Positive(message = "Provider ID must be a positive number")
    private Long providerId;

    private Boolean allowProjectParticipation;

    private List<String> tags;

    @Valid
    private List<ServicePackageRequest> packages;
}
