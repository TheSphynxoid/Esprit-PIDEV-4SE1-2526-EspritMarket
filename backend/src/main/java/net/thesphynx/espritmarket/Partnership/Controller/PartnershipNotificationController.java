package net.thesphynx.espritmarket.Partnership.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import net.thesphynx.espritmarket.Partnership.Entity.Notification;
import net.thesphynx.espritmarket.Partnership.Service.PartnershipNotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/partnership/notifications")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PartnershipNotificationController {

    private final PartnershipNotificationService service;

    /**
     * Get all notifications for a user (sorted by date desc)
     */
    @GetMapping("/{userId}")
    public List<Notification> getUserNotifications(@PathVariable Long userId) {
        return service.getUserNotifications(userId);
    }

    /**
     * Get unread count for a user
     */
    @GetMapping("/{userId}/unread-count")
    public Map<String, Long> getUnreadCount(@PathVariable Long userId) {
        return Map.of("count", service.getUnreadCount(userId));
    }

    /**
     * Mark a single notification as read
     */
    @PutMapping("/{id}/read")
    public Notification markAsRead(@PathVariable Long id) {
        return service.markAsRead(id);
    }

    /**
     * Mark all notifications as read for a user
     */
    @PutMapping("/{userId}/read-all")
    public void markAllAsRead(@PathVariable Long userId) {
        service.markAllAsRead(userId);
    }
}
