package net.thesphynx.espritmarket.Partnership.Entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime interviewDate;

    @Column(nullable = false)
    private String type;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, columnDefinition = "varchar(255) default 'SCHEDULED'")
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    private InterviewResult result;

    @Column(columnDefinition = "TEXT")
    private String resultNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    @JsonIgnore
    private Application application;

    @JsonProperty("studentFirstName")
    public String getStudentFirstName() {
        if (application == null || application.getApplicant() == null) {
            return "Unknown";
        }
        String fullName = application.getApplicant().getName();
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Unknown";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "Unknown";
    }

    @JsonProperty("studentLastName")
    public String getStudentLastName() {
        if (application == null || application.getApplicant() == null) {
            return "";
        }
        String fullName = application.getApplicant().getName();
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length > 1) {
            return String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        }
        return "";
    }

    @JsonProperty("jobTitle")
    public String getJobTitle() {
        if (application == null || application.getJobOffer() == null) {
            return "Unknown";
        }
        String title = application.getJobOffer().getTitle();
        return title != null && !title.trim().isEmpty() ? title : "Unknown";
    }

    @JsonProperty("applicationId")
    public Long getApplicationId() {
        return application != null ? application.getId() : null;
    }
}