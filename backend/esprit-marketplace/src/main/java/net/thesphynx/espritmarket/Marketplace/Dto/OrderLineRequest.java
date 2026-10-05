package net.thesphynx.espritmarket.Marketplace.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderLineRequest {

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private Double price;

    // orderId est résolu automatiquement
    private Long orderId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    private String dimensionsLabel;
    private Double weight;
}