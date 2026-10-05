package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleRequestDto {
    @NotNull(message = "Proposed date is required")
    private LocalDateTime proposedDate;

    @NotNull(message = "Proposed duration is required")
    @Positive(message = "Proposed duration must be positive")
    private double proposedDuration;

    private String reason;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;
}
