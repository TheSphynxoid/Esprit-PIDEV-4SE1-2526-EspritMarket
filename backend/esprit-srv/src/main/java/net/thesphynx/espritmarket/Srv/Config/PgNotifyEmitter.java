package net.thesphynx.espritmarket.Srv.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.thesphynx.espritmarket.Common.Event.NotificationEvent;
import net.thesphynx.espritmarket.Common.Event.StatusTransitionEvent;
import net.thesphynx.espritmarket.Srv.Service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@Component
public class PgNotifyEmitter {

    private static final Logger log = LoggerFactory.getLogger(PgNotifyEmitter.class);
    private static final String CHANNEL_BOOKING = "srv_booking_event";
    private static final String CHANNEL_NOTIFICATION = "srv_notification";

    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationService notificationService;

    public PgNotifyEmitter(DataSource dataSource, NotificationService notificationService) {
        this.dataSource = dataSource;
        this.notificationService = notificationService;
    }

    @Async
    @EventListener
    public void onStatusTransition(StatusTransitionEvent event) {
        if (!"Booking".equals(event.getEntityType())) return;
        emit(CHANNEL_BOOKING, Map.of(
                "type", "STATUS_TRANSITION",
                "entityId", event.getEntityId(),
                "entityType", event.getEntityType(),
                "fromStatus", event.getFromStatus() != null ? event.getFromStatus() : "NEW",
                "toStatus", event.getToStatus() != null ? event.getToStatus() : "UNKNOWN",
                "changedBy", event.getChangedBy() != null ? event.getChangedBy() : 0
        ));
    }

    @Async
    @EventListener
    public void onNotification(NotificationEvent event) {
        notificationService.create(
                event.getRecipientId(),
                event.getType(),
                event.getTitle(),
                event.getMessage(),
                event.getRelatedEntityId(),
                event.getRelatedEntityType()
        );
        emit(CHANNEL_NOTIFICATION, Map.of(
                "type", event.getType(),
                "recipientId", event.getRecipientId(),
                "title", event.getTitle(),
                "message", event.getMessage(),
                "relatedEntityId", event.getRelatedEntityId() != null ? event.getRelatedEntityId() : 0,
                "relatedEntityType", event.getRelatedEntityType() != null ? event.getRelatedEntityType() : ""
        ));
    }

    @SuppressWarnings("java:S2077")
    private void emit(String channel, Map<String, Object> payload) {
        if (!isValidChannel(channel)) {
            log.warn("Rejected pg_notify on invalid channel: {}", channel);
            return;
        }
        try (Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            String json = objectMapper.writeValueAsString(payload);
            String escaped = json.replace("'", "''");
            stmt.execute("NOTIFY " + channel + ", '" + escaped + "'");
        } catch (Exception e) {
            log.warn("pg_notify emission failed on {}: {}", channel, e.getMessage());
        }
    }

    private boolean isValidChannel(String channel) {
        return channel != null && channel.matches("^[a-z_][a-z0-9_]{0,62}$");
    }
}
