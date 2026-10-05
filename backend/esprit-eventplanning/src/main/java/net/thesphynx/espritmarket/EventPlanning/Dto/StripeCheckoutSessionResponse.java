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
public class StripeCheckoutSessionResponse {
    
    private String sessionId;
    private String clientSecret;
    private String publishableKey;
    private Long ticketId;
    private String message;
}
