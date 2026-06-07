package net.thesphynx.espritmarket.EventPlanning.Controller;

import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Dto.StripeCheckoutSessionRequest;
import net.thesphynx.espritmarket.EventPlanning.Dto.StripeCheckoutSessionResponse;
import net.thesphynx.espritmarket.EventPlanning.Service.StripePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/eventplanning/payments")
@Tag(name = "EventPlanning - Stripe Payments")
@Slf4j
public class StripePaymentController {
    
    private final StripePaymentService stripePaymentService;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    public StripePaymentController(StripePaymentService stripePaymentService) {
        this.stripePaymentService = stripePaymentService;
    }
    
    /**
     * Create a Stripe Checkout Session for a ticket
     */
    @PostMapping("/checkout-session")
    @Operation(summary = "Create Stripe Checkout Session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Checkout session created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Ticket not found"),
        @ApiResponse(responseCode = "500", description = "Stripe API error")
    })
    public ResponseEntity<StripeCheckoutSessionResponse> createCheckoutSession(
            @Valid @RequestBody StripeCheckoutSessionRequest request) {
        try {
            StripeCheckoutSessionResponse response = stripePaymentService.createCheckoutSession(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating checkout session: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Stripe Webhook endpoint to handle payment events
     * Handles: checkout.session.completed, charge.failed, etc.
     */
    @PostMapping("/webhook")
    @Operation(summary = "Handle Stripe Webhook Events")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Webhook processed"),
        @ApiResponse(responseCode = "400", description = "Invalid signature or payload")
    })
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        try {
            // Verify webhook signature
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            
            log.info("Received Stripe webhook event: {}", event.getType());
            
            // Handle different event types
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "charge.failed":
                    handleChargeFailed(event);
                    break;
                case "charge.dispute.created":
                    handleChargeDispute(event);
                    break;
                default:
                    log.info("Unhandled event type: {}", event.getType());
            }
            
            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Webhook Error: " + e.getMessage());
        }
    }
    
    /**
     * Handle successful checkout session completion
     */
    private void handleCheckoutSessionCompleted(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (session != null && session.getPaymentIntent() != null) {
                String sessionId = session.getId();
                String paymentIntentId = session.getPaymentIntent();
                
                log.info("✅ Checkout session completed: {} | Payment Intent: {}", sessionId, paymentIntentId);
                
                // Update ticket payment status
                stripePaymentService.handlePaymentSuccess(sessionId, paymentIntentId);
            }
        } catch (Exception e) {
            log.error("Error handling checkout.session.completed: {}", e.getMessage());
        }
    }
    
    /**
     * Handle charge failure events
     */
    private void handleChargeFailed(Event event) {
        try {
            // The charge object contains metadata with sessionId
            log.warn("⚠️ Charge failed event received");
            // Could implement additional logic here for failed payment handling
        } catch (Exception e) {
            log.error("Error handling charge.failed: {}", e.getMessage());
        }
    }
    
    /**
     * Handle charge dispute events
     */
    private void handleChargeDispute(Event event) {
        try {
            log.warn("⚠️ Charge dispute created");
            // Could implement additional logic here for dispute handling
        } catch (Exception e) {
            log.error("Error handling charge.dispute.created: {}", e.getMessage());
        }
    }
}
