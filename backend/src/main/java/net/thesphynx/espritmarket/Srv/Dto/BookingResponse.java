package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingResponse {
    private Long id;
    private LocalDateTime date;
    private double duration;
    private BookingStatus status;
    private String notes;
    private Long userId;
    private String userName;
    private Long serviceId;
    private String serviceName;
    private Long providerId;
    private String providerName;
    private Long partnerId;
    private BigDecimal totalPrice;
    private BigDecimal priorityMarkup;
    private boolean highPriority;
    private Long projectId;
    private String projectTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
