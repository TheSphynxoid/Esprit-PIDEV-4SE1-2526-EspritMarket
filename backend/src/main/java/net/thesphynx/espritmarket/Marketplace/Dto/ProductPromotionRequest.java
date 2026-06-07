package net.thesphynx.espritmarket.Marketplace.Dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductPromotionRequest {

    @NotNull(message = "discountPercent is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "discountPercent must be greater than 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "discountPercent must be less or equal to 100")
    private Double discountPercent;

    @NotNull(message = "promoEndAt is required")
    private LocalDateTime promoEndAt;
}
