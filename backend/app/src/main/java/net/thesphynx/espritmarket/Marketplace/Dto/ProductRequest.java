package net.thesphynx.espritmarket.Marketplace.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private Double price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @NotBlank(message = "Product image is required")
    @Pattern(
        regexp = "^(https?://.*|data:image/.*)?$",
        message = "Image URL must be a valid HTTP/HTTPS URL or data:image URI"
    )
    private String imageUrl;

    private Long storeId;
    private Long categoryId;

    @Size(max = 100, message = "Dimensions label must not exceed 100 characters")
    private String dimensionsLabel;

    @DecimalMin(value = "0.0", inclusive = false, message = "Weight must be greater than 0")
    private Double weight;

    @Valid
    private RelationRef store;

    @Valid
    private RelationRef category;

    public Long resolveStoreId() {
        if (storeId != null) {
            return storeId;
        }
        return store != null ? store.getId() : null;
    }

    public Long resolveCategoryId() {
        if (categoryId != null) {
            return categoryId;
        }
        return category != null ? category.getId() : null;
    }

    @Data
    public static class RelationRef {
        private Long id;
    }
}