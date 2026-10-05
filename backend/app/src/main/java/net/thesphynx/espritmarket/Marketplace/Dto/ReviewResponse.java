package net.thesphynx.espritmarket.Marketplace.Dto;

import lombok.Data;

@Data
public class ReviewResponse {
    private Long id;
    private String comment;
    private int rating;
    private Long productId;
    private String productName;
    private Long userId;
}
