package net.thesphynx.espritmarket.Srv.Config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PgNotifyListener {

    private static final Logger log = LoggerFactory.getLogger(PgNotifyListener.class);
    private static final String[] CHANNELS = {"srv_booking_event", "srv_notification"};

    private final DataSource dataSource;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${srv.pg-notify.enabled:true}")
    private boolean enabled;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;

    public PgNotifyListener(DataSource dataSource,
                            SimpMessagingTemplate messagingTemplate) {
        this.dataSource = dataSource;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    void start() {
        if (!enabled) {
            log.info("pg_notify listener disabled");
            return;
        }

        running.set(true);
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pg-notify-listener");
            t.setDaemon(true);
            return t;
        });

        executor.submit(this::listen);
        log.info("pg_notify listener started on channels: {}", (Object) CHANNELS);
    }

    @PreDestroy
    void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void listen() {
        while (running.get()) {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(true);
                try (Statement stmt = conn.createStatement()) {
                    for (String channel : CHANNELS) {
                        if (channel.matches("^[a-z_][a-z0-9_]{0,62}$")) {
                            stmt.execute("LISTEN " + channel);
                        }
                    }
                }

                Class<?> pgConnectionClass = Class.forName("org.postgresql.PGConnection");
                Method unwrap = conn.getClass().getMethod("unwrap", Class.class);
                Object pgConn = unwrap.invoke(conn, pgConnectionClass);
                Method getNotifications = pgConnectionClass.getMethod("getNotifications", int.class);

                while (running.get()) {
                    Object[] notifications = (Object[]) getNotifications.invoke(pgConn, 5000);

                    if (notifications != null) {
                        Method getName = notifications.getClass().getComponentType().getMethod("getName");
                        Method getParameter = notifications.getClass().getComponentType().getMethod("getParameter");
                        for (Object notification : notifications) {
                            String name = (String) getName.invoke(notification);
                            String param = (String) getParameter.invoke(notification);
                            handleNotification(name, param);
                        }
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    log.warn("pg_notify listener connection lost, reconnecting in 5s: {}", e.getMessage());
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
    }

    private void handleNotification(String channel, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.has("type") ? node.get("type").asText() : "UNKNOWN";

            switch (channel) {
                case "srv_booking_event" -> {
                    Long entityId = node.has("entityId") ? node.get("entityId").asLong() : null;
                    messagingTemplate.convertAndSend("/topic/bookings/" + entityId, payload);
                    messagingTemplate.convertAndSend("/topic/bookings", payload);
                    log.debug("pg_notify booking event: entityId={}, type={}", entityId, type);
                }
                case "srv_notification" -> {
                    Long recipientId = node.has("recipientId") ? node.get("recipientId").asLong() : null;
                    if (recipientId != null) {
                        messagingTemplate.convertAndSend("/topic/notifications/" + recipientId, payload);
                    }
                    log.debug("pg_notify notification: recipientId={}", recipientId);
                }
                default -> log.debug("pg_notify unhandled channel: {}", channel);
            }
        } catch (Exception e) {
            log.warn("Failed to handle pg_notify payload on {}: {}", channel, e.getMessage());
        }
    }
}
