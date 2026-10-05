package net.thesphynx.espritmarket.EventPlanning.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Dto.StripeCheckoutSessionRequest;
import net.thesphynx.espritmarket.EventPlanning.Dto.StripeCheckoutSessionResponse;
import net.thesphynx.espritmarket.EventPlanning.Entity.Ticket;
import net.thesphynx.espritmarket.EventPlanning.Repository.ITicketRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class StripePaymentService {
    
    private final ITicketRepository ticketRepository;
    
    @Value("${stripe.api.key}")
    private String stripeApiKey;
    
    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;
    
    @Value("${app.stripe.success-url:http://localhost:4200/payment-success}")
    private String successUrl;
    
    @Value("${app.stripe.cancel-url:http://localhost:4200/payment-cancel}")
    private String cancelUrl;
    
    public StripePaymentService(ITicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }
    
    /**
     * Create a Stripe Checkout Session for ticket purchase
     */
    @Transactional
    public StripeCheckoutSessionResponse createCheckoutSession(StripeCheckoutSessionRequest request) {
        try {
            Stripe.apiKey = stripeApiKey;
            
            // Get ticket from database
            Optional<Ticket> ticketOpt = ticketRepository.findById(request.getTicketId());
            if (ticketOpt.isEmpty()) {
                throw new RuntimeException("Ticket not found: " + request.getTicketId());
            }
            
            Ticket ticket = ticketOpt.get();
            
            // Amount should be in cents (e.g., 25.00 USD = 2500 cents)
            Long amountInCents = (long) (request.getAmount() * 100);
            
            // Create line item for the ticket
            SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(request.getCurrency() != null ? request.getCurrency() : "usd")
                            .setUnitAmount(amountInCents)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Ticket - " + ticket.getType() + " (Event #" + request.getEventId() + ")")
                                    .setDescription("Event ticket purchase")
                                    .build()
                            )
                            .build()
                    )
                    .build();
            
            // Create checkout session parameters
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .addLineItem(lineItem)
                    .setSuccessUrl(request.getSuccessUrl() != null ? request.getSuccessUrl() : successUrl)
                    .setCancelUrl(request.getCancelUrl() != null ? request.getCancelUrl() : cancelUrl)
                    .putMetadata("ticketId", String.valueOf(request.getTicketId()))
                    .putMetadata("eventId", String.valueOf(request.getEventId()))
                    .putMetadata("ticketType", request.getTicketType())
                    .build();
            
            // Create the session
            Session session = Session.create(params);
            
            // Save session ID to ticket
            ticket.setStripeSessionId(session.getId());
            ticket.setPaymentStatus("PENDING");
            ticketRepository.save(ticket);
            
            log.info("✅ Stripe Checkout Session created: {} for Ticket ID: {}", session.getId(), request.getTicketId());
            
            return StripeCheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .clientSecret(session.getClientSecret())
                    .publishableKey(stripePublishableKey)
                    .ticketId(request.getTicketId())
                    .message("Checkout session created successfully")
                    .build();
                    
        } catch (StripeException e) {
            log.error("❌ Stripe API Error: {}", e.getMessage());
            throw new RuntimeException("Failed to create checkout session: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ Error creating checkout session: {}", e.getMessage());
            throw new RuntimeException("Failed to create checkout session: " + e.getMessage());
        }
    }
    
    /**
     * Handle successful payment from Stripe webhook
     */
    @Transactional
    public void handlePaymentSuccess(String sessionId, String paymentIntentId) {
        try {
            Optional<Ticket> ticketOpt = ticketRepository.findByStripeSessionId(sessionId);
            
            if (ticketOpt.isPresent()) {
                Ticket ticket = ticketOpt.get();
                ticket.setPaymentStatus("COMPLETED");
                ticket.setStripePaymentIntentId(paymentIntentId);
                ticketRepository.save(ticket);
                
                log.info("✅ Payment completed for Ticket ID: {} | Session: {} | Payment Intent: {}", 
                    ticket.getId(), sessionId, paymentIntentId);
            } else {
                log.warn("⚠️ Ticket not found for session: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("❌ Error handling payment success: {}", e.getMessage());
        }
    }
    
    /**
     * Handle failed payment from Stripe webhook
     */
    @Transactional
    public void handlePaymentFailure(String sessionId) {
        try {
            Optional<Ticket> ticketOpt = ticketRepository.findByStripeSessionId(sessionId);
            
            if (ticketOpt.isPresent()) {
                Ticket ticket = ticketOpt.get();
                ticket.setPaymentStatus("FAILED");
                ticketRepository.save(ticket);
                
                log.info("❌ Payment failed for Ticket ID: {} | Session: {}", ticket.getId(), sessionId);
            }
        } catch (Exception e) {
            log.error("❌ Error handling payment failure: {}", e.getMessage());
        }
    }
}
