package net.thesphynx.espritmarket.Partnership.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import net.thesphynx.espritmarket.Partnership.Entity.Notification;
import net.thesphynx.espritmarket.Partnership.Entity.NotificationType;
import net.thesphynx.espritmarket.Partnership.Repository.NotificationRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("partnershipNotificationService")
public class PartnershipNotificationService {

    private final NotificationRepository repository;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public PartnershipNotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Notification sendNotification(Long userId, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setType(type);
        Notification saved = repository.save(notification);

        if (messagingTemplate == null) {
            log.warn("WebSocket not available, skipping real-time push for notification to user {}: {}", userId, message);
            return saved;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", saved.getId());
            payload.put("message", saved.getMessage());
            payload.put("type", saved.getType().name());
            payload.put("createdAt", saved.getCreatedAt().toString());
            payload.put("isRead", false);
            messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                (Object) payload
            );
            log.info("Notification pushed to user {}: {}", userId, message);
        } catch (Exception e) {
            log.warn("Failed to push notification via WebSocket to user {}: {}", userId, e.getMessage());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return repository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        return repository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = repository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        repository.saveAll(unread);
    }
}
