package net.thesphynx.espritmarket.Srv.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "escrow_hold", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"booking_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EscrowHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @JsonIgnoreProperties({"bookings", "messages", "attachments", "project", "deliverables"})
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @JsonIgnoreProperties({"user", "transactions"})
    private Wallet wallet;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "release_deadline")
    private LocalDateTime releaseDeadline;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
