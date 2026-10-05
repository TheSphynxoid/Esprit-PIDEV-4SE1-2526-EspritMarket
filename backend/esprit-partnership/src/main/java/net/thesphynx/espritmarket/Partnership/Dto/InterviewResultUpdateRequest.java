package net.thesphynx.espritmarket.Partnership.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.thesphynx.espritmarket.Partnership.Entity.InterviewResult;
import net.thesphynx.espritmarket.Partnership.Entity.InterviewStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResultUpdateRequest {

    @NotNull(message = "Interview status is required")
    private InterviewStatus status;

    private InterviewResult result;

    @Size(max = 1000, message = "Result notes must not exceed 1000 characters")
    private String resultNotes;
}