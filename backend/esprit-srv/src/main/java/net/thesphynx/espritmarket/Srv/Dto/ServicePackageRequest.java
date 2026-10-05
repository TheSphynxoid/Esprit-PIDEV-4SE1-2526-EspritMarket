package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServicePackageRequest {

    @NotBlank(message = "Package tier is required")
    private String tier;

    @NotBlank(message = "Package name is required")
    @Size(max = 100, message = "Package name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Package description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Package price is required")
    @DecimalMin(value = "1.0", message = "Price must be at least 1.0")
    @DecimalMax(value = "100000.0", message = "Price must not exceed 100000.0")
    private BigDecimal price;

    @Min(value = 1, message = "Delivery days must be at least 1")
    @Max(value = 365, message = "Delivery days must not exceed 365")
    private Integer deliveryDays = 7;

    @Min(value = 0, message = "Revisions must be 0 or more")
    private Integer revisions = 1;

    private List<String> features;

    private boolean recommended;
}
