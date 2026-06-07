package net.thesphynx.espritmarket.Marketplace.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Double originalPrice;
    private Double discountPercent;
    private LocalDateTime promoStartAt;
    private LocalDateTime promoEndAt;
    private Boolean isPromotionActive;
    private Double discountedPrice;
    private Long remainingSeconds;
    private String promotionStatus;
    private Integer stock;
    private String imageUrl;
    private Long storeId;
    private String storeName;
    private Long categoryId;
    private String categoryName;
    private String dimensionsLabel;
    private Double weight;
    private Integer soldQuantity;
}
