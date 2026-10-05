package net.thesphynx.espritmarket.Marketplace.Dto;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class OrderResponse {
    private Long id;
    private Date date;
    private String status;
    private Double totalAmount;

 

    private Long userId;
    private List<OrderLineResponse> orderLines;
}