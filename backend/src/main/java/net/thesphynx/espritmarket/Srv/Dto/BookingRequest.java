package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull(message = "Booking date is required")
    @Future(message = "Booking date must be in the future")
    private LocalDateTime date;

    @DecimalMin(value = "0.1", message = "Duration must be at least 0.1 hours")
    @Max(value = 720, message = "Duration must not exceed 720 hours (30 days)")
    private double duration;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @NotNull(message = "Service ID is required")
    @Positive(message = "Service ID must be a positive number")
    private Long serviceId;

    private Long partnerId;

    private Long projectId;

    private boolean highPriority = false;

    private Long packageId;
}
