package net.thesphynx.espritmarket.EventPlanning.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentReservationRequest {

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "reservationId is required")
    private Long reservationId;

    @NotNull(message = "equipmentId is required")
    private Long equipmentId;

    @NotNull(message = "stallId is required")
    private Long stallId;
}