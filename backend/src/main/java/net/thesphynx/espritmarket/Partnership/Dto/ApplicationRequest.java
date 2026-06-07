package net.thesphynx.espritmarket.Partnership.Dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "PENDING|ACCEPTED|REJECTED",
            message = "Status must be PENDING, ACCEPTED, or REJECTED")
    private String status;

    @PositiveOrZero(message = "Matching score must be >= 0")
    @Max(value = 100, message = "Matching score cannot exceed 100")
    private double matchingScore;

    @NotNull(message = "Applicant ID is required")
    @Positive(message = "Applicant ID must be positive")
    private Long applicantId;

    @NotNull(message = "Job offer ID is required")
    @Positive(message = "Job offer ID must be positive")
    private Long jobOfferId;

    private String motivation;

    // Profile Snapshot Fields
    private java.util.List<String> skills;
    private String experienceLevel;
    private String fieldOfStudy;
    private String yearsOfExperience;
    private java.util.List<String> languages;
}
