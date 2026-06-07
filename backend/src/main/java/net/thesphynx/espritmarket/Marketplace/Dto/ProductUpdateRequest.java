package net.thesphynx.espritmarket.Marketplace.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductUpdateRequest {

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

    @Size(max = 5500000, message = "Image URL is too long")
    private String imageUrl;

    private Long storeId;
    private Long categoryId;
    private String dimensionsLabel;
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