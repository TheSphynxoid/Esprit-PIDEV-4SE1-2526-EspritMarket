package net.thesphynx.espritmarket.Marketplace.Dto;

import lombok.Data;

@Data
public class OrderLineResponse {
    private Long id;
    private int quantity;
    private Double price;
    private Double subtotal;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productImage;
    private String dimensionsLabel;
    private Double weight;
}