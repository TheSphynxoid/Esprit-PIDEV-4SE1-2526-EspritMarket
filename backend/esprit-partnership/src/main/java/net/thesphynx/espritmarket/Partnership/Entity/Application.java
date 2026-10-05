package net.thesphynx.espritmarket.Partnership.Entity;

import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String status;  // PENDING, ACCEPTED, REJECTED

    private double matchingScore;

    @Column(columnDefinition = "TEXT")
    private String motivation;

    // ── Profile Snapshot Fields ──────────────────────────
    @ElementCollection
    @CollectionTable(name = "application_skills", joinColumns = @JoinColumn(name = "application_id"))
    private List<String> skills = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level")
    private ExperienceLevel experienceLevel;

    @Column(name = "field_of_study")
    private String fieldOfStudy;

    @Column(name = "years_of_experience")
    private String yearsOfExperience;

    @ElementCollection
    @CollectionTable(name = "application_languages", joinColumns = @JoinColumn(name = "application_id"))
    private List<String> languages = new ArrayList<>();

    // ── Activity Tracking Fields ─────────────────────────
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_candidate_action_at")
    private LocalDateTime lastCandidateActionAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_status")
    private ApplicationActivityStatus activityStatus = ApplicationActivityStatus.ACTIVE;

    @Column(name = "is_flagged")
    private Boolean flagged = false;

    // ── Relationships ────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_offer_id", nullable = false)
    private JobOffer jobOffer;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Interview> interviews = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastCandidateActionAt = LocalDateTime.now();
        this.activityStatus = ApplicationActivityStatus.ACTIVE;
    }
}