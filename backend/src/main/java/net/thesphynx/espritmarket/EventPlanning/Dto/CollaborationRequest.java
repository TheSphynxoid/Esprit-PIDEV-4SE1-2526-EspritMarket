package net.thesphynx.espritmarket.EventPlanning.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollaborationRequest {

    @NotBlank(message = "name is required")
    @Size(max = 50, message = "name must not exceed 50 characters")
    private String name;

    @NotBlank(message = "type is required")
    private String type;

    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;

    @NotNull(message = "eventId is required")
    private Long eventId;
}