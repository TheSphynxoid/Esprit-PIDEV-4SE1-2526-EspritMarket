package net.thesphynx.espritmarket.EventPlanning.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentRequest {

    @NotBlank(message = "name is required")
    @Pattern(
            regexp = "^(Chairs|Tables|Microphone|Speaker|Projector|Screen|Lighting|Stage|Tent)$",
            message = "name must be valid equipment (Chairs, Tables, Microphone, Speaker, Projector, Screen, Lighting, Stage, Tent)"
    )
    private String name;

    @NotBlank(message = "type is required")
    @Pattern(
            regexp = "^(Furniture|Audio|Decoration|Visual|Lighting|Structure)$",
            message = "type must be Furniture, Audio, Decoration, Visual, Lighting, or Structure"
    )
    private String type;

    @NotBlank(message = "status is required")
    @Pattern(
            regexp = "^(AVAILABLE|IN_USE|MAINTENANCE)$",
            message = "status must be AVAILABLE, IN_USE, or MAINTENANCE"
    )
    private String status;

    @NotNull(message = "eventId is required")
    private Long eventId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity = 1;

    private String imageUrl;
}