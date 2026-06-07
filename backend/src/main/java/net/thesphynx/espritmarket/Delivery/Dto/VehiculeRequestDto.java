package net.thesphynx.espritmarket.Delivery.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehiculeRequestDto {
    @NotBlank(message = "type is required")
    @Pattern(
            regexp = "(?i)^(motor|car|truck)$",
            message = "type must be motor, car, or truck"
    )
    private String type;

    @NotBlank(message = "registrationnumbers is required")
    @Pattern(
            regexp = "^(\\d{5}|\\d{3}\\sTU\\s\\d{4})$",
            message = "registrationnumbers must be 5 digits for motor or '### TU ####' for car/truck"
    )
    private String registrationnumbers;

    @NotNull(message = "capacity is required")
    @Positive(message = "capacity must be positive")
    private Double capacity;

    @NotBlank(message = "status is required")
    @Pattern(
            regexp = "(?i)^(available|unavailable)$",
            message = "status must be available or unavailable"
    )
    private String status;
}
