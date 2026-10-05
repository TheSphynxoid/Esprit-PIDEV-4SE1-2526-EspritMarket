package net.thesphynx.espritmarket.Partnership.Dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRequest {

    @NotNull(message = "Interview date is required")
    @FutureOrPresent(message = "Interview date cannot be in the past")
    private LocalDateTime interviewDate;

    @NotBlank(message = "Interview type is required")
    @Pattern(regexp = "PHONE|VIDEO|IN_PERSON",
            message = "Type must be PHONE, VIDEO, or IN_PERSON")
    private String type;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Size(max = 500, message = "Result must not exceed 500 characters")
    private String result;

    @Size(max = 1000, message = "Result notes must not exceed 1000 characters")
    private String resultNotes;

    @NotNull(message = "Application ID is required")
    @Positive(message = "Application ID must be positive")
    private Long applicationId;
}
