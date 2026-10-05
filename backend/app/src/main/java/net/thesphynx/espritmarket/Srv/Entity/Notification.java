package net.thesphynx.espritmarket.Srv.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notification", indexes = {
        @Index(name = "idx_notif_recipient_read", columnList = "recipient_id, is_read"),
        @Index(name = "idx_notif_recipient_created", columnList = "recipient_id, created_at")
})
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String message;

    @Column(nullable = false)
    private String priority = "MEDIUM";

    private Long relatedEntityId;

    @Column(length = 50)
    private String relatedEntityType;

    @Column(nullable = false)
    private boolean isRead = false;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
