package net.thesphynx.espritmarket.Common.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    // ✅ mock-mode=false en production pour envoyer vraiment les emails
    @Value("${app.email.mock-mode:false}")
    private boolean mockMode;

    public EmailService(JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendEmail(String to, String subject, String body) {
        if (mockMode) {
            // Mode développement : affiche dans les logs seulement
            logger.info("📧 [MOCK EMAIL] To: {} | Subject: {} | Body: {}", to, subject, body);
            System.out.println("📧 [MOCK EMAIL] To: " + to);
            System.out.println("📧 [MOCK EMAIL] Code visible: " + body);
            return;
        }

        // Mode production : envoie vraiment l'email via Gmail SMTP
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(fromAddress)) {
            message.setFrom(fromAddress);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            logger.info("✅ Email sent successfully to: {}", to);
        } catch (MailException ex) {
            logger.error("❌ SMTP send failed for recipient {}", to, ex);
            throw new RuntimeException("Failed to send email: " + ex.getMessage(), ex);
        }
    }

    // ─── Code de vérification ────────────────────────────────────────
    public void sendVerificationCode(String to, String code) {
        String subject = "ESPRIT Market - Code de vérification";
        String body = String.format(
                "Bonjour,\n\n" +
                "Votre code de vérification pour créer votre boutique sur ESPRIT Market est :\n\n" +
                "          🔑  %s\n\n" +
                "Ce code est valable pendant 10 minutes.\n\n" +
                "Si vous n'avez pas fait cette demande, ignorez cet email.\n\n" +
                "Cordialement,\n" +
                "L'équipe ESPRIT Market",
                code);
        sendEmail(to, subject, body);
        logger.info("📨 Verification code sent to: {} (mockMode={})", to, mockMode);
    }

    // ─── Email d'approbation ─────────────────────────────────────────
    public void sendApprovalEmail(String to, String name, Long userId) {
        String subject = "Votre demande de boutique a été approuvée !";
        String body = String.format(
                "Bonjour %s,\n\n" +
                "Félicitations ! Votre demande pour ouvrir une boutique sur ESPRIT Market a été approuvée.\n\n" +
                "Vous pouvez maintenant compléter la configuration de votre boutique :\n" +
                "http://localhost:4200/create-store-step-2\n\n" +
                "À bientôt sur ESPRIT Market !",
                name);
        sendEmail(to, subject, body);
    }

    // ─── Email de rejet ──────────────────────────────────────────────
    public void sendRejectionEmail(String to, String name) {
        String subject = "Mise à jour de votre demande de boutique";
        String body = String.format(
                "Bonjour %s,\n\n" +
                "Nous avons bien reçu votre demande pour ouvrir une boutique sur ESPRIT Market.\n\n" +
                "Malheureusement, votre demande a été refusée pour le moment.\n" +
                "Vous pouvez nous contacter pour plus d'informations.\n\n" +
                "Cordialement,\nL'équipe ESPRIT Market",
                name);
        sendEmail(to, subject, body);
    }
}