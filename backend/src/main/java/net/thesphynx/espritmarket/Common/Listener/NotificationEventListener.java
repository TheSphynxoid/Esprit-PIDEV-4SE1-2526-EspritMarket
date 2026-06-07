package net.thesphynx.espritmarket.Common.Listener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Event.NotificationEvent;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Common.Service.EmailService;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onNotificationEvent(NotificationEvent event) {
        if (event == null || event.getRecipientId() == null) {
            return;
        }

        userRepository.findById(event.getRecipientId()).ifPresentOrElse(
                user -> dispatchNotification(user, event),
                () -> log.warn("Notification recipient not found: recipientId={}", event.getRecipientId())
        );
    }

    private void dispatchNotification(User user, NotificationEvent event) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendEmail(user.getEmail(), event.getTitle(), event.getMessage());
            } catch (Exception ex) {
                log.error("Failed to send notification email to userId={} email={}", user.getId(), user.getEmail(), ex);
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", event.getRecipientId());
        payload.put("type", event.getType());
        payload.put("title", event.getTitle());
        payload.put("message", event.getMessage());
        payload.put("relatedEntityId", event.getRelatedEntityId());
        payload.put("relatedEntityType", event.getRelatedEntityType());
        payload.put("timestamp", Instant.now().toString());
//imp
        messagingTemplate.convertAndSend("/topic/notifications/users/" + user.getId(), (Object) payload);
        log.info("Notification pushed to userId={} type={}", user.getId(), event.getType());
    }
}