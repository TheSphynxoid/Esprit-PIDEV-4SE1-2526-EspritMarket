package net.thesphynx.espritmarket.Partnership.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import net.thesphynx.espritmarket.Partnership.Dto.InterviewUpdateRequest;
import net.thesphynx.espritmarket.Partnership.Entity.Application;
import net.thesphynx.espritmarket.Partnership.Entity.Interview;
import net.thesphynx.espritmarket.Partnership.Entity.InterviewResult;
import net.thesphynx.espritmarket.Partnership.Entity.InterviewStatus;
import net.thesphynx.espritmarket.Partnership.Entity.NotificationType;
import net.thesphynx.espritmarket.Partnership.Repository.ApplicationRepository;
import net.thesphynx.espritmarket.Partnership.Repository.InterviewRepository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewService {

    private final InterviewRepository repository;
    private final ApplicationRepository applicationRepository;
    private final PartnershipNotificationService notificationService;

    public Interview create(Interview interview) {
        Application application = loadApplication(interview);
        interview.setApplication(application);
        return repository.save(interview);
    }

    @Transactional(readOnly = true)
    public List<Interview> getAll() {
        return repository.findAllWithApplicationDetails();
    }

    @Transactional(readOnly = true)
    public Interview getById(Long id) {
        return repository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Interview not found"));
    }

    @Transactional
    public Interview update(Long id, InterviewUpdateRequest request) {
        Interview existing = getById(id);

        if (request.getInterviewDate() != null) {
            existing.setInterviewDate(request.getInterviewDate());
        }

        if (!isBlank(request.getType())) {
            existing.setType(request.getType().trim());
        }

        if (request.getLocation() != null) {
            String location = request.getLocation().trim();
            existing.setLocation(location.isEmpty() ? null : location);
        }

        InterviewStatus status = null;
        if (!isBlank(request.getStatus())) {
            status = parseStatus(request.getStatus());
            existing.setStatus(status);
        }

        if (request.getResult() != null) {
            String resultValue = request.getResult().trim();
            if (resultValue.isEmpty()) {
                existing.setResult(null);
            } else {
                existing.setResult(parseResult(resultValue));
            }
        } else if (status != null && status != InterviewStatus.COMPLETED) {
            existing.setResult(null);
        }

        if (status == InterviewStatus.COMPLETED && isBlank(request.getResult())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Result is required when status is COMPLETED"
            );
        }

        if (request.getResultNotes() != null) {
            String notes = request.getResultNotes().trim();
            existing.setResultNotes(notes.isEmpty() ? null : notes);
        }

        Interview saved = repository.save(existing);

        // Auto-update linked Application status
        if (saved.getResult() != null) {
            Application app = saved.getApplication();
            if (app != null) {
                boolean statusChanged = false;
                if (saved.getResult() == InterviewResult.ACCEPTED) {
                    app.setStatus("ACCEPTED");
                    statusChanged = true;
                } else if (saved.getResult() == InterviewResult.REJECTED) {
                    app.setStatus("REJECTED");
                    statusChanged = true;
                } else if (saved.getResult() == InterviewResult.WAITING_LIST) {
                    app.setStatus("WAITING_LIST");
                    statusChanged = true;
                }
                
                if (statusChanged) {
                    applicationRepository.save(app);
                    
                    // 🔔 Notify student about interview result
                    try {
                        Long studentId = app.getApplicant().getId();
                        String resultText = saved.getResult().name();
                        notificationService.sendNotification(
                            studentId,
                            "📋 Résultat d'entretien: " + resultText + " pour votre candidature",
                            NotificationType.RESULT
                        );
                    } catch (Exception e) {
                        System.err.println("Failed to send result notification: " + e.getMessage());
                    }
                }
            }
        }

        return saved;
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<LocalDateTime> suggestOptimalSlots(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        if (application.getJobOffer() == null || application.getJobOffer().getCompany() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application is not linked to a valid company");
        }

        Long companyId = application.getJobOffer().getCompany().getId();
        LocalDateTime now = LocalDateTime.now();

        // Get all upcoming scheduled interviews for this company
        List<Interview> upcomingInterviews = repository.findUpcomingScheduledInterviewsByCompany(companyId, now);

        List<LocalDateTime> suggestedSlots = new ArrayList<>();
        
        // Start looking from tomorrow at 09:00
        LocalDateTime cursor = now.plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        
        // Safety counter to prevent infinite loop
        int maxDaysToSearch = 30; 
        int daysSearched = 0;
        LocalDateTime searchLimit = now.plusDays(maxDaysToSearch);

        while (suggestedSlots.size() < 3 && cursor.isBefore(searchLimit)) {
            // Skip weekends
            if (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
                cursor = cursor.plusDays(1).withHour(9);
                daysSearched++;
                continue;
            }

            // Move to next day if we are past 16:00 (since an interview takes 1 hour, last slot is 16:00)
            if (cursor.getHour() >= 17) {
                cursor = cursor.plusDays(1).withHour(9);
                daysSearched++;
                continue;
            }

            // Check if cursor conflicts with any existing interview
            boolean conflict = false;
            for (Interview interview : upcomingInterviews) {
                if (interview.getInterviewDate() != null) {
                    long minutesDifference = Math.abs(ChronoUnit.MINUTES.between(interview.getInterviewDate(), cursor));
                    if (minutesDifference < 60) {
                        conflict = true;
                        break;
                    }
                }
            }

            if (!conflict) {
                suggestedSlots.add(cursor);
            }

            // Increment by 1 hour for the next slot candidate
            cursor = cursor.plusHours(1);
        }

        return suggestedSlots;
    }

    private Application loadApplication(Interview interview) {
        Long applicationId = interview.getApplication() != null
                ? interview.getApplication().getId()
                : null;

        if (applicationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application ID is required");
        }

        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private InterviewStatus parseStatus(String value) {
        try {
            return InterviewStatus.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interview status");
        }
    }

    private InterviewResult parseResult(String value) {
        try {
            return InterviewResult.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interview result");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}