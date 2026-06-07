package net.thesphynx.espritmarket.Srv.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.User;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ServiceReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String comment;
    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"serviceReviews", "projects", "services", "bookings", "partner"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    @JsonIgnoreProperties({"user", "partner"})
    private Booking booking;

    // ML Sentiment Analysis fields
    private String sentiment; // e.g. POSITIVE, NEUTRAL, NEGATIVE
    @Column(name = "sentiment_confidence", columnDefinition = "NUMERIC(5,4)")
    private Double sentimentConfidence;
}
