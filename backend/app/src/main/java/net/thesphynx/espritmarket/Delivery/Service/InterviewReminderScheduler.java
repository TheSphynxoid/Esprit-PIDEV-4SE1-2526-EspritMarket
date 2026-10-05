package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Delivery.Entity.QuizResult;
import net.thesphynx.espritmarket.Delivery.Entity.QuizStatus;
import net.thesphynx.espritmarket.Delivery.Repository.IQuizResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class InterviewReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(InterviewReminderScheduler.class);
    private static final int REMINDER_LEAD_MINUTES = 5;

    private final IQuizResultRepository quizResultRepository;
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:no-reply@espritmarket.local}")
    private String interviewMailFrom;

    public InterviewReminderScheduler(IQuizResultRepository quizResultRepository,
                                      JavaMailSender javaMailSender) {
        this.quizResultRepository = quizResultRepository;
        this.javaMailSender = javaMailSender;
    }

    @Scheduled(cron = "${quiz.scheduler.interview-reminder-cron:0 * * * * *}")
    @Transactional
    public void sendInterviewReminders() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime reminderStart = now.plusMinutes(REMINDER_LEAD_MINUTES);
        LocalDateTime reminderEndExclusive = reminderStart.plusMinutes(1);

        List<QuizResult> upcomingInterviews = quizResultRepository
                .findByStatusAndInterviewScheduledAtGreaterThanEqualAndInterviewScheduledAtLessThanAndInterviewReminderSentAtIsNullOrderByInterviewScheduledAtAsc(
                        QuizStatus.ACCEPTED_QUIZ,
                        reminderStart,
                        reminderEndExclusive);

        if (upcomingInterviews.isEmpty()) {
            return;
        }

        for (QuizResult result : upcomingInterviews) {
            try {
                sendReminderEmail(result);
                result.setInterviewReminderSentAt(LocalDateTime.now());
                quizResultRepository.save(result);
            } catch (Exception exception) {
                log.warn("Unable to send interview reminder for quiz result {}: {}", result.getId(), exception.getMessage());
            }
        }
    }

    private void sendReminderEmail(QuizResult result) {
        if (result.getUser() == null || result.getUser().getEmail() == null || result.getUser().getEmail().isBlank()) {
            throw new IllegalArgumentException("Utilisateur ou email introuvable");
        }

        String fullName = buildFullName(result.getUser().getName());
        String dateText = result.getInterviewScheduledAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(interviewMailFrom);
        message.setTo(result.getUser().getEmail());
        message.setSubject("Esprit Market - Rappel d'entretien dans 5 minutes");
        message.setText(
                "Bonjour " + fullName + ",\n\n" +
                "Vous avez un entretien dans 5 minutes.\n\n" +
                "Nom: " + fullName + "\n" +
                "Date et heure: " + dateText + "\n\n" +
                "Merci de vous connecter a l'heure.\n\n" +
                "Cordialement,\n" +
                "Esprit Market"
        );

        javaMailSender.send(message);
    }

    private String buildFullName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "Utilisateur";
        }

        return rawName.trim().replaceAll("\\s+", " ");
    }
}