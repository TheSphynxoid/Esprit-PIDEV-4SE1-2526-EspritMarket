package net.thesphynx.espritmarket.EventPlanning.Dto;

import jakarta.validation.constraints.Max;
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
public class TicketPromoSelectionRequest {

    @NotNull(message = "eventId is required")
    private Long eventId;

    @NotNull(message = "discountPercent is required")
    @Min(value = 0, message = "discountPercent must be between 0 and 100")
    @Max(value = 100, message = "discountPercent must be between 0 and 100")
    private Integer discountPercent;

    private String discountLabel;
}