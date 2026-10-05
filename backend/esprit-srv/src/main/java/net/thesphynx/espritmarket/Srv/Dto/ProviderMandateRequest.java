package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProviderMandateRequest {
    @NotNull(message = "Provider ID is required")
    @Positive(message = "Provider ID must be positive")
    private Long providerId;

    @NotNull(message = "Max bookings is required")
    @Min(value = 1, message = "Max bookings must be at least 1")
    private int maxBookings;
}
