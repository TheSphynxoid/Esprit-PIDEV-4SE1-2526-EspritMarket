package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingMessageResponse {
    private Long id;
    private Long bookingId;
    private Long senderId;
    private String senderName;
    private String message;
    private LocalDateTime createdAt;
}
