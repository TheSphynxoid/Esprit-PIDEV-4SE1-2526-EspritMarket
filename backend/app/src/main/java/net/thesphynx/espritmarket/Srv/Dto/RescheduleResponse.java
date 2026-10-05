package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleResponse {
    private Long id;
    private Long bookingId;
    private Long requestedById;
    private String requestedByName;
    private LocalDateTime originalDate;
    private double originalDuration;
    private LocalDateTime proposedDate;
    private double proposedDuration;
    private String reason;
    private String message;
    private String status;
    private Long respondedById;
    private String respondedByName;
    private LocalDateTime respondedAt;
    private String responseMessage;
    private LocalDateTime createdAt;
}
