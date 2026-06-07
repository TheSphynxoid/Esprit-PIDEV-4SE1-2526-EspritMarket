package net.thesphynx.espritmarket.EventPlanning.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.User;

@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Double price;

    @Column(name = "original_price")
    private Double originalPrice;

    @Column(name = "final_price")
    private Double finalPrice;

    @Column(name = "discount_applied", nullable = false)
    @Builder.Default
    private boolean discountApplied = false;

    @Column(name = "discount_rate")
    private Double discountRate;

    @Column(name = "discount_label")
    private String discountLabel;

    @Column(name = "payment_status", nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'PENDING'")
    private String paymentStatus = "PENDING";  // PENDING, COMPLETED, FAILED

    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    @JsonIgnoreProperties({"stalls", "collaborations", "reservations", "equipments", "tickets"})
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
