package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Srv.Dto.NotificationResponse;
import net.thesphynx.espritmarket.Srv.Dto.NotificationSummaryResponse;
import net.thesphynx.espritmarket.Srv.Entity.Notification;
import net.thesphynx.espritmarket.Srv.Repository.INotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NotificationService {

    private final INotificationRepository notificationRepository;

    private static final Map<String, String> TYPE_PRIORITY = Map.ofEntries(
            Map.entry("BOOKING_REJECTED", "HIGH"),
            Map.entry("BOOKING_DISPUTED", "HIGH"),
            Map.entry("DELIVERABLE_REJECTED", "HIGH"),
            Map.entry("BOOKING_CONFIRMED", "MEDIUM"),
            Map.entry("BOOKING_COMPLETED", "MEDIUM"),
            Map.entry("BOOKING_PENDING_REVIEW", "HIGH"),
            Map.entry("DELIVERABLE_SUBMITTED", "MEDIUM"),
            Map.entry("DELIVERABLE_APPROVED", "LOW"),
            Map.entry("BOOKING_TENTATIVE", "LOW"),
            Map.entry("BOOKING_PENDING_EVALUATION", "MEDIUM"),
            Map.entry("BOOKING_APPROVED", "MEDIUM"),
            Map.entry("RESCHEDULE_REQUESTED", "HIGH"),
            Map.entry("DELIVERABLE_REVISION_REQUESTED", "HIGH")
    );

    public NotificationService(INotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void create(Long recipientId, String type, String title, String message,
                      Long relatedEntityId, String relatedEntityType) {
        String priority = TYPE_PRIORITY.getOrDefault(type, "MEDIUM");
        Notification notif = new Notification();
        notif.setRecipientId(recipientId);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setPriority(priority);
        notif.setRelatedEntityId(relatedEntityId);
        notif.setRelatedEntityType(relatedEntityType);
        notificationRepository.save(notif);
    }

    public PageResponse<NotificationResponse> getNotifications(Long recipientId, int page, int size) {
        Page<Notification> p = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, PageRequest.of(page, size));
        return toPageResponse(p);
    }

    public NotificationSummaryResponse getSummary(Long recipientId) {
        long unread = notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
        return NotificationSummaryResponse.builder()
                .unreadCount(unread)
                .totalCount(0)
                .build();
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.markAsRead(id);
    }

    @Transactional
    public void markAllAsRead(Long recipientId) {
        notificationRepository.markAllAsRead(recipientId);
    }

    private PageResponse<NotificationResponse> toPageResponse(Page<Notification> p) {
        List<NotificationResponse> content = p.getContent().stream().map(this::toResponse).toList();
        return PageResponse.of(content, p.getNumber(), p.getSize(), p.getTotalElements());
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setRecipientId(n.getRecipientId());
        r.setType(n.getType());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setPriority(n.getPriority());
        r.setRelatedEntityId(n.getRelatedEntityId());
        r.setRelatedEntityType(n.getRelatedEntityType());
        r.setRead(n.isRead());
        r.setReadAt(n.getReadAt());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
}
