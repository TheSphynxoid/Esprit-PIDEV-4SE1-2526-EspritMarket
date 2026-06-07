package net.thesphynx.espritmarket.Partnership.Dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewUpdateRequest {

    @FutureOrPresent(message = "Interview date cannot be in the past")
    private LocalDateTime interviewDate;

    @Pattern(regexp = "PHONE|VIDEO|IN_PERSON",
            message = "Type must be PHONE, VIDEO, or IN_PERSON")
    private String type;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Pattern(regexp = "SCHEDULED|COMPLETED|CANCELLED",
            message = "Status must be SCHEDULED, COMPLETED, or CANCELLED")
    private String status;

    @Pattern(regexp = "ACCEPTED|REJECTED|WAITING_LIST",
            message = "Result must be ACCEPTED, REJECTED, or WAITING_LIST")
    private String result;

    @Size(max = 1000, message = "Result notes must not exceed 1000 characters")
    private String resultNotes;
}
