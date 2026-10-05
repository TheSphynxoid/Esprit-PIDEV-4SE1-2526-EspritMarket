package net.thesphynx.espritmarket.Partnership.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Partnership.Dto.ApplicationRequest;
import net.thesphynx.espritmarket.Partnership.Entity.Application;
import net.thesphynx.espritmarket.Partnership.Entity.JobOffer;
import net.thesphynx.espritmarket.Partnership.Entity.NotificationType;
import net.thesphynx.espritmarket.Partnership.Service.ApplicationService;
import net.thesphynx.espritmarket.Partnership.Service.PartnershipNotificationService;
import net.thesphynx.espritmarket.Partnership.Service.JobOfferService;
import net.thesphynx.espritmarket.Partnership.Service.AiMatchingService;
import net.thesphynx.espritmarket.Partnership.Dto.AiPredictRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/api/partnership/applications")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ApplicationController {

    private final ApplicationService service;
    private final PartnershipNotificationService notificationService;
    private final JobOfferService jobOfferService;
    private final AiMatchingService aiMatchingService;
    @PostMapping("/apply")
    public Application apply(@Valid @RequestBody ApplicationRequest request) {
        Application application = toEntity(request);
        if (application.getStatus() == null || application.getStatus().isBlank()) {
            application.setStatus("PENDING");
        }
        
        // 🤖 Call AI Matching Service
        try {
            JobOffer fullOffer = jobOfferService.getById(request.getJobOfferId());
            AiPredictRequest aiRequest = AiPredictRequest.builder()
                .cv_skills(request.getSkills() != null ? request.getSkills() : new java.util.ArrayList<>())
                .cv_experience_level(request.getExperienceLevel() != null ? request.getExperienceLevel() : "BEGINNER")
                .cv_field_of_study(request.getFieldOfStudy() != null ? request.getFieldOfStudy() : "N/A")
                .cv_years_of_experience(request.getYearsOfExperience() != null ? request.getYearsOfExperience() : "0-1")
                .cv_languages(request.getLanguages() != null ? request.getLanguages() : new java.util.ArrayList<>())
                .job_required_skills(fullOffer.getRequiredSkills() != null ? java.util.Arrays.asList(fullOffer.getRequiredSkills().split(",")) : new java.util.ArrayList<>())
                .job_experience_level(fullOffer.getExperienceLevel() != null ? fullOffer.getExperienceLevel().name() : "BEGINNER")
                .build();
            
            double score = aiMatchingService.getMatchingScore(aiRequest);
            application.setMatchingScore(score);
            
            // 🔔 Notify recruiter about new application
            notificationService.sendNotification(
                1L, // TODO: replace with actual recruiter userId
                "📨 Nouvelle candidature pour l'offre: " + fullOffer.getTitle(),
                net.thesphynx.espritmarket.Partnership.Entity.NotificationType.APPLICATION
            );
        } catch (Exception e) {
            System.err.println("Failed to process AI matching or notification: " + e.getMessage());
            application.setMatchingScore(0);
        }

        return service.create(application);
    }



    @GetMapping
    public List<Application> getAll() {
        return service.getAll();
    }

    @GetMapping("/student/{applicantId}")
    public List<Application> getByApplicantId(@PathVariable Long applicantId) {
        return service.getByApplicantId(applicantId);
    }

    @GetMapping("/{id}")
    public Application getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Application update(@PathVariable Long id,
                              @Valid @RequestBody ApplicationRequest request) {
        return service.update(id, toEntity(request));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // ── Inactivity Detection Endpoints ───────────────────

    /**
     * Get all flagged (INACTIVE or AT_RISK) applications.
     * Sorted by urgency: AT_RISK first, then INACTIVE.
     */
    @GetMapping("/inactive")
    public List<Application> getInactiveApplications() {
        return service.getFlaggedApplications();
    }

    /**
     * Manually refresh a candidate's activity timestamp.
     * Resets the candidate to ACTIVE status.
     */
    @PatchMapping("/{id}/refresh-activity")
    public void refreshActivity(@PathVariable Long id) {
        service.refreshCandidateActivity(id);
    }

    private Application toEntity(ApplicationRequest request) {
        Application application = new Application();
        application.setStatus(request.getStatus());
        application.setMatchingScore(request.getMatchingScore());
        application.setMotivation(request.getMotivation());

        User user = new User();
        user.setId(request.getApplicantId());
        application.setApplicant(user);

        JobOffer jobOffer = new JobOffer();
        jobOffer.setId(request.getJobOfferId());
        application.setJobOffer(jobOffer);

        // Map Profile Snapshot Fields
        application.setSkills(request.getSkills() != null ? request.getSkills() : new java.util.ArrayList<>());
        if (request.getExperienceLevel() != null && !request.getExperienceLevel().isBlank()) {
            try {
                application.setExperienceLevel(net.thesphynx.espritmarket.Partnership.Entity.ExperienceLevel.valueOf(request.getExperienceLevel().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid enum
            }
        }
        application.setFieldOfStudy(request.getFieldOfStudy());
        application.setYearsOfExperience(request.getYearsOfExperience());
        application.setLanguages(request.getLanguages() != null ? request.getLanguages() : new java.util.ArrayList<>());

        return application;
    }
}