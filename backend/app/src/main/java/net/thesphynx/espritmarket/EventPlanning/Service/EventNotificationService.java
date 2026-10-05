package net.thesphynx.espritmarket.EventPlanning.Service;

import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EventNotificationService {
    
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    @Value("${app.event-notification.also-send-to:}")
    private String extraRecipients;
    @Value("${app.event-notification.teams-link-prefix:https://teams.microsoft.com/l/meetup-join/}")
    private String teamsLinkPrefix;
    
    public EventNotificationService(UserRepository userRepository, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }
    
    /**
     * Send email notifications to all @esprit.tn users when a new event is created
     * especially for online events with Teams meeting links
     */
    public int notifyUsersForOnlineEvent(Event event) {
        if (event == null || !event.isOnline()) {
            log.warn("⚠️ Event is null or not online, skipping notifications");
            return 0;
        }
        
        try {
            Set<String> recipients = new LinkedHashSet<>();

            // Notify all users with a valid email, plus any extra Outlook recipients configured in application.properties
            userRepository.findAll().stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .forEach(recipients::add);

            Arrays.stream(extraRecipients == null ? new String[0] : extraRecipients.split(","))
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .forEach(recipients::add);

            log.info("📧 Found {} recipients to notify", recipients.size());
            
            int count = 0;
            for (String recipient : recipients) {
                if (sendNotification(recipient, event)) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            log.error("❌ Error sending notifications: {}", e.getMessage());
            return 0;
        }
    }
    
    private boolean sendNotification(String recipientEmail, Event event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject("Un nouvel evenement en ligne a ete cree.");
            helper.setText(buildEmailHtml(event), true);
            
            mailSender.send(message);
            log.info("✅ Email sent to: {}", recipientEmail);
            return true;
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", recipientEmail, e.getMessage());
            return false;
        }
    }
    
    private String buildEmailHtml(Event event) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:Arial, sans-serif;color:#222222;font-size:15px;line-height:1.6;margin:0;padding:0;'>")
            .append("<div style='max-width:680px;margin:0 auto;padding:24px 16px;'>")
            .append("<p style='margin:0 0 18px 0;'>Un nouvel evenement en ligne a ete cree.</p>")
            .append("<p style='margin:0 0 8px 0;'>Nom: ").append(escapeHtml(event.getName())).append("</p>")
            .append("<p style='margin:0 0 8px 0;'>Date: ").append(escapeHtml(event.getDate() != null ? event.getDate().toString().replace('-', '/') : "")).append("</p>");

        String meetingLink = resolveMeetingLink(event);
        if (!meetingLink.isBlank()) {
            String link = escapeHtml(meetingLink);
            html.append("<p style='margin:0 0 10px 0;'>Lien de reunion Teams:</p>")
                .append("<p style='margin:0 0 18px 0;'>")
                .append("<a href=\"").append(link).append("\" target=\"_blank\" rel=\"noreferrer\" style=\"display:inline-block;color:#2563eb;text-decoration:underline;word-break:break-word;\">")
                .append(link)
                .append("</a></p>")
                .append("<p style='margin:0 0 20px 0;'>")
                .append("<a href=\"").append(link).append("\" target=\"_blank\" rel=\"noreferrer\" style=\"display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;padding:10px 16px;border-radius:8px;font-weight:600;\">")
                .append("Rejoindre la reunion Teams")
                .append("</a></p>");
        }

        html.append("<p style='margin:0 0 8px 0;'>Cordialement,</p>")
            .append("<p style='margin:0;'>Equipe Esprit Market</p>")
            .append("</div>")
                .append("</body></html>");

        return html.toString();
    }

    private String resolveMeetingLink(Event event) {
        if (event.getMeetingLink() != null && !event.getMeetingLink().isBlank()) {
            return event.getMeetingLink().trim();
        }

        String rawContext = event.getId() + "|" + event.getName() + "|" + event.getDate();
        String encodedContext = URLEncoder.encode(rawContext, StandardCharsets.UTF_8);
        String prefix = teamsLinkPrefix == null ? "" : teamsLinkPrefix.trim();

        if (prefix.isBlank()) {
            return "";
        }

        if (prefix.endsWith("/")) {
            return prefix + "?context=" + encodedContext;
        }

        return prefix + "?context=" + encodedContext;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Send alert email to event creator when event is today or tomorrow (1 day away)
     * Works for all event types (online and in-person)
     */
    public boolean notifyCreatorForUpcomingEvent(Event event) {
        if (event == null || event.getCreator() == null || event.getCreator().getEmail() == null) {
            log.warn("⚠️ Cannot notify: Event or creator email is missing");
            return false;
        }

        try {
            String creatorEmail = event.getCreator().getEmail();
            if (creatorEmail.isBlank()) {
                log.warn("⚠️ Creator email is blank for event: {}", event.getName());
                return false;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            helper.setTo(creatorEmail);
            helper.setSubject("⏰ ALERTE: Votre evenement arrive bientôt");
            helper.setText(buildUpcomingEventAlertHtml(event), true);
            
            mailSender.send(message);
            log.info("✅ Upcoming event alert sent to creator: {}", creatorEmail);
            return true;
        } catch (Exception e) {
            log.error("❌ Failed to send upcoming event alert for event '{}': {}", event.getName(), e.getMessage());
            return false;
        }
    }

    private String buildUpcomingEventAlertHtml(Event event) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate eventDate = event.getDate();
        boolean isToday = eventDate.equals(today);
        boolean isTomorrow = eventDate.equals(today.plusDays(1));
        
        String timingMessage = isToday ? "AUJOURD'HUI ⏰" : "DEMAIN ⏰";
        String timingFr = isToday ? "aujourd'hui" : "demain";

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:Arial, sans-serif;color:#222222;font-size:15px;line-height:1.6;margin:0;padding:0;'>")
            .append("<div style='max-width:680px;margin:0 auto;padding:24px 16px;'>")
            .append("<div style='background-color:#fff3cd;border-left:4px solid #ff6b6b;padding:16px;margin:0 0 20px 0;border-radius:4px;'>")
            .append("<p style='margin:0;color:#d63031;font-weight:bold;font-size:16px;'>")
            .append("⚠️ ALERTE: Votre evenement se deroule ").append(timingMessage).append("</p>")
            .append("</div>")
            .append("<p style='margin:0 0 12px 0;'>Bonjour,</p>")
            .append("<p style='margin:0 0 18px 0;'>")
            .append("Votre evenement <strong>").append(escapeHtml(event.getName())).append("</strong> se deroulera <strong>")
            .append(timingFr).append("</strong> (").append(escapeHtml(eventDate.toString().replace('-', '/'))).append(").")
            .append("</p>")
            .append("<div style='background-color:#f5f5f5;padding:16px;margin:0 0 18px 0;border-radius:8px;'>")
            .append("<p style='margin:0 0 8px 0;'><strong>Détails de l'événement:</strong></p>")
            .append("<p style='margin:0 0 6px 0;'><strong>Type:</strong> ").append(event.isOnline() ? "En ligne (Teams)" : "En présentiel").append("</p>")
            .append("<p style='margin:0 0 6px 0;'><strong>Lieu:</strong> ").append(escapeHtml(event.getLocation())).append("</p>")
            .append("<p style='margin:0;'><strong>Tickets vendus:</strong> ").append(event.getNbTickets()).append("</p>")
            .append("</div>")
            .append("<p style='margin:0 0 18px 0;'>")
            .append("Veuillez vous assurer que tout est pret pour accueillir les participants.")
            .append("</p>")
            .append("<p style='margin:0 0 8px 0;'>Cordialement,</p>")
            .append("<p style='margin:0;'>Equipe Esprit Market</p>")
            .append("</div>")
            .append("</body></html>");

        return html.toString();
    }
}
