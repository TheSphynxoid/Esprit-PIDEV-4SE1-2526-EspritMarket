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
public class StripeCheckoutSessionRequest {
    
    private Long ticketId;
    private Long eventId;
    private String ticketType;
    private Double amount;  // Amount in cents
    private String currency;  // e.g., "usd", "eur"
    private String successUrl;  // Redirect URL after successful payment
    private String cancelUrl;   // Redirect URL if payment is cancelled
}
