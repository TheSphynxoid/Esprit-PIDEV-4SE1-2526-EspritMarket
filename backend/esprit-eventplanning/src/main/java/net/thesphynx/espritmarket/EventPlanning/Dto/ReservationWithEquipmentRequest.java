package net.thesphynx.espritmarket.EventPlanning.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationWithEquipmentRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "date is required")
    private LocalDate date;

    @NotNull(message = "eventId is required")
    private Long eventId;

    @NotNull(message = "equipment list is required")
    private List<EquipmentWithQuantity> equipments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquipmentWithQuantity {
        @NotNull(message = "equipmentId is required")
        private Long equipmentId;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        private Integer quantity;

        private Long stallId;
    }
}
