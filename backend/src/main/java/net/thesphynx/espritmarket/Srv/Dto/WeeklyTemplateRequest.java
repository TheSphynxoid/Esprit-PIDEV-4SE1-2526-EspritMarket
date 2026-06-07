package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class WeeklyTemplateRequest {
    @NotNull(message = "Provider ID is required")
    @Positive(message = "Provider ID must be positive")
    private Long providerId;

    @Positive(message = "Service ID must be positive")
    private Long serviceId;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start hour is required")
    private LocalTime startHour;

    @NotNull(message = "End hour is required")
    private LocalTime endHour;

    @Min(value = 15, message = "Slot duration must be at least 15 minutes")
    @Max(value = 480, message = "Slot duration must not exceed 480 minutes")
    private int slotDurationMinutes = 60;

    @Min(value = 1, message = "Max concurrent must be at least 1")
    @Max(value = 100, message = "Max concurrent must not exceed 100")
    private int maxConcurrent = 1;
}
