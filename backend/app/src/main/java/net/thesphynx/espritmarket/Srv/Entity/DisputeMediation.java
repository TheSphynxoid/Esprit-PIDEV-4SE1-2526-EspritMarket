package net.thesphynx.espritmarket.Srv.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispute_mediation", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"booking_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DisputeMediation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_resolution", nullable = false, length = 50)
    private ResolutionType suggestedResolution;

    @Column(name = "suggested_refund_percent")
    private Double suggestedRefundPercent;

    @Column(name = "suggested_deadline_extension_days")
    private Integer suggestedDeadlineExtensionDays;

    @Column(name = "analysis_summary", length = 1000)
    private String analysisSummary;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private VoteType clientVote;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private VoteType providerVote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MediationStatus status;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

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

    public enum ResolutionType {
        PARTIAL_REFUND, FULL_REFUND, EXTEND_DEADLINE, REASSIGN_PROVIDER, NO_ACTION
    }

    public enum VoteType {
        ACCEPT, REJECT, ABSTAIN
    }

    public enum MediationStatus {
        PENDING, IN_PROGRESS, RESOLVED, ESCALATED
    }
}
