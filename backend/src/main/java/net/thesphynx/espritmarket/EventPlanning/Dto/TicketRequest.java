package net.thesphynx.espritmarket.EventPlanning.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequest {

    @NotBlank(message = "type is required")
    @Size(max = 50, message = "type must not exceed 50 characters")
    @Pattern(
            regexp = "^(VIP|REGULAR|STUDENT)$",
            message = "type must be VIP, REGULAR, or STUDENT"
    )
    private String type;

    @NotNull(message = "price is required")
    @Positive(message = "price must be positive")
    private Double price;

    @Positive(message = "originalPrice must be positive")
    private Double originalPrice;

    @NotNull(message = "discountApplied is required")
    private Boolean discountApplied = false;

    private Double discountRate;

    private String discountLabel;

    @NotNull(message = "eventId is required")
    private Long eventId;

    private Long userId;
}