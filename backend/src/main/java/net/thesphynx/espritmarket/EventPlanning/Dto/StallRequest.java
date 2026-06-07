package net.thesphynx.espritmarket.EventPlanning.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StallRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @NotNull(message = "number is required")
    @Min(value = 1, message = "number must be at least 1")
    private Integer number;

    @NotBlank(message = "location is required")
    @Pattern(
            regexp = "^(A|B|C|D|E|IJK|M|G)$",
            message = "location must be one of: A, B, C, D, E, IJK, M, G"
    )
    private String location;

    @NotNull(message = "eventId is required")
    private Long eventId;

    private Long userId;
}