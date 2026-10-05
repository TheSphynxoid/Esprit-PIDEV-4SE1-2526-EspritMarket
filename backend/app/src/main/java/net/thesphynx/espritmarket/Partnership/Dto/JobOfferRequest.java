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
public class JobOfferRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "INTERNSHIP|PROJECT|SERVICE",
            message = "Type must be INTERNSHIP, PROJECT, or SERVICE")
    private String type;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "OPEN|CLOSED",
            message = "Status must be OPEN or CLOSED")
    private String status;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Experience level is required")
    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED",
            message = "Experience level must be BEGINNER, INTERMEDIATE, or ADVANCED")
    private String experienceLevel;

    @Size(max = 1000, message = "Required skills must not exceed 1000 characters")
    private String requiredSkills;

    @NotNull(message = "Company ID is required")
    @Positive(message = "Company ID must be positive")
    private Long companyId;
}
