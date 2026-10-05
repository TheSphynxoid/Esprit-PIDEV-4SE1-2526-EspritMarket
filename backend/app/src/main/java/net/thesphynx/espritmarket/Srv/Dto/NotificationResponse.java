package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private Long recipientId;
    private String type;
    private String title;
    private String message;
    private String priority;
    private Long relatedEntityId;
    private String relatedEntityType;
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
