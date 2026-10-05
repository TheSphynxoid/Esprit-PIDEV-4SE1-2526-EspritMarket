package net.thesphynx.espritmarket.Marketplace.Dto;

import java.util.List;

import lombok.Data;

@Data
public class OrderDeliveryResponse {

    private Long orderId;


    private String status;
    private List<OrderDeliveryLineDto> products;

    @Data
    public static class OrderDeliveryLineDto {
        private Long productId;
        private String productName;
        private int quantity;
        private Double price;
        private Double subtotal;
        private String dimensionsLabel;
        private Double weight;
    }
}