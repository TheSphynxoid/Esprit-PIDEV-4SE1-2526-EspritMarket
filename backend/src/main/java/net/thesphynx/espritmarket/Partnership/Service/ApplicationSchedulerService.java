package net.thesphynx.espritmarket.Partnership.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import net.thesphynx.espritmarket.Partnership.Entity.Application;
import net.thesphynx.espritmarket.Partnership.Entity.ApplicationActivityStatus;
import net.thesphynx.espritmarket.Partnership.Entity.Interview;
import net.thesphynx.espritmarket.Partnership.Entity.NotificationType;
import net.thesphynx.espritmarket.Partnership.Repository.ApplicationRepository;
import net.thesphynx.espritmarket.Partnership.Repository.InterviewRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApplicationSchedulerService {

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final PartnershipNotificationService notificationService;

    /**
     * SCHEDULER TASK 1: Auto-update application status
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoUpdateApplicationStatus() {
        System.out.println("🕐 [SCHEDULER STARTED] Auto-updating application statuses...");

        List<Application> pendingApps = applicationRepository.findByStatus("PENDING");
        LocalDateTime today = LocalDateTime.now();

        for (Application app : pendingApps) {
            boolean hasUpcomingInterview = app.getInterviews().stream()
                    .anyMatch(interview -> interview.getInterviewDate().isAfter(today));

            if (!hasUpcomingInterview && app.getInterviews().isEmpty()) {
                app.setStatus("EXPIRED");
                System.out.println("  📌 Application ID " + app.getId() + " marked as EXPIRED (no interviews)");
            }
        }

        List<Interview> passedInterviews = interviewRepository.findByInterviewDateBefore(today);
        for (Interview interview : passedInterviews) {
            Application app = interview.getApplication();
            if (interview.getResult() != null && app.getStatus().equals("PENDING")) {
                app.setStatus("INTERVIEW_COMPLETED");
                System.out.println("  ✅ Application ID " + app.getId() + " updated to INTERVIEW_COMPLETED");
            }
        }

        applicationRepository.saveAll(pendingApps);
        System.out.println("✅ [SCHEDULER COMPLETED] Application status update finished!");
    }

    // ══════════════════════════════════════════════════════════════
    //  SCHEDULER TASK 2: Candidate Inactivity Detection
    //  Runs every hour — updates activityStatus for all applications
    // ══════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 * * * *")  // Every hour
    @Transactional
    public void detectInactiveCandidates() {
        System.out.println("🔍 [INACTIVITY DETECTOR] Starting scan...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveDaysAgo = now.minusDays(5);
        LocalDateTime fortyEightHoursAgo = now.minusHours(48);

        // Track all processed application IDs to avoid double-processing
        Set<Long> processed = new HashSet<>();

        // ── RULE 1: AT_RISK ──────────────────────────────────────
        // Interview scheduled BUT no candidate action within 48 hours
        List<Application> atRiskApps = applicationRepository.findAtRiskCandidates(fortyEightHoursAgo);
        for (Application app : atRiskApps) {
            app.setActivityStatus(ApplicationActivityStatus.AT_RISK);
            app.setFlagged(true);
            processed.add(app.getId());
            System.out.println("  ⚠️  Application ID " + app.getId() + " → AT_RISK (interview pending, no action in 48h)");
        }
        applicationRepository.saveAll(atRiskApps);

        // ── RULE 2: INACTIVE ─────────────────────────────────────
        // No candidate activity for more than 5 days
        List<Application> inactiveApps = applicationRepository.findInactiveCandidates(fiveDaysAgo);
        for (Application app : inactiveApps) {
            if (!processed.contains(app.getId())) {  // Don't downgrade AT_RISK to INACTIVE
                app.setActivityStatus(ApplicationActivityStatus.INACTIVE);
                app.setFlagged(true);
                processed.add(app.getId());
                System.out.println("  💤 Application ID " + app.getId() + " → INACTIVE (no action in 5+ days)");
            }
        }
        applicationRepository.saveAll(inactiveApps);

        // ── RULE 3: ACTIVE ───────────────────────────────────────
        // Reset any previously flagged applications that are now active again
        List<Application> allPending = applicationRepository.findByStatus("PENDING");
        for (Application app : allPending) {
            if (!processed.contains(app.getId()) && Boolean.TRUE.equals(app.getFlagged())) {
                app.setActivityStatus(ApplicationActivityStatus.ACTIVE);
                app.setFlagged(false);
                System.out.println("  ✅ Application ID " + app.getId() + " → ACTIVE (candidate resumed activity)");
            }
        }
        applicationRepository.saveAll(allPending);

        System.out.println("✅ [INACTIVITY DETECTOR] Scan complete. Processed " + processed.size() + " flagged applications.");
    }

    // ══════════════════════════════════════════════════════════════
    //  SCHEDULER TASK 3: Interview Reminder (15 minutes before)
    //  Runs every minute
    // ══════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 * * * * *")  // Every minute
    @Transactional(readOnly = true)
    public void sendInterviewReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in15Minutes = now.plusMinutes(15);

        List<Interview> upcomingInterviews = interviewRepository.findUpcomingInterviewsBetween(now, in15Minutes);

        for (Interview interview : upcomingInterviews) {
            try {
                Application app = interview.getApplication();
                if (app != null && app.getApplicant() != null) {
                    Long studentId = app.getApplicant().getId();
                    String jobTitle = app.getJobOffer() != null ? app.getJobOffer().getTitle() : "N/A";
                    
                    notificationService.sendNotification(
                        studentId,
                        "⏰ Rappel: Votre entretien pour '" + jobTitle + "' commence dans 15 minutes!",
                        NotificationType.INTERVIEW_REMINDER
                    );
                    
                    // Also notify recruiter
                    notificationService.sendNotification(
                        1L, // TODO: replace with actual recruiter userId
                        "⏰ Rappel: Entretien avec " + app.getApplicant().getName() + " dans 15 minutes",
                        NotificationType.INTERVIEW_REMINDER
                    );

                    System.out.println("  ⏰ Reminder sent for interview ID " + interview.getId());
                }
            } catch (Exception e) {
                System.err.println("Failed to send interview reminder: " + e.getMessage());
            }
        }
    }
}
