package net.thesphynx.espritmarket.EventPlanning.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketPromoOfferResponse {
    private String date;
    private String label;
    private double discountRate;
    private String discountLabel;
}