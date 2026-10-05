package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.ProviderExceptionType;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ProviderExceptionRequest {
    @NotNull(message = "Provider ID is required")
    @Positive(message = "Provider ID must be positive")
    private Long providerId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Exception type is required")
    private ProviderExceptionType type;

    private LocalTime startHour;

    private LocalTime endHour;

    @Size(max = 200, message = "Reason must not exceed 200 characters")
    private String reason;
}
