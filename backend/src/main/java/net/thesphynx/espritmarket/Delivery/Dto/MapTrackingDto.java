package net.thesphynx.espritmarket.Delivery.Dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MapTrackingDto {
    @NotBlank(message = "currentLocation is required")
    private String currentLocation;

    @NotNull(message = "lastUpdate is required")
    @PastOrPresent(message = "lastUpdate must be in the past or present")
    private LocalDateTime lastUpdate;

    @NotNull(message = "estimatedArrival is required")
    @FutureOrPresent(message = "estimatedArrival must be in the present or future")
    private LocalDateTime estimatedArrival;
}
