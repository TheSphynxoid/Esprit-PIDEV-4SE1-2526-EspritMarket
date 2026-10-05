package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceReviewResponse {
    private Long id;
    private String comment;
    private BigDecimal rating;
    private Long userId;
    private String userName;
    private Long serviceId;
    private Long bookingId;
    
    private String sentiment;
    private Double sentimentConfidence;
}
