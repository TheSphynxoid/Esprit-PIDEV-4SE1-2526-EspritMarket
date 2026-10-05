package net.thesphynx.espritmarket.Delivery.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "role"})
    private User user;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private QuizStatus status; // PENDING, ACCEPTED_QUIZ, REJECTED

    private String meetingLink; // lien entretien (Jitsi ou autre)

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime interviewScheduledAt;

    private LocalDateTime interviewReminderSentAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
