package net.thesphynx.espritmarket.Srv.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "slot_allocation_audit")
public class SlotAllocationAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long serviceId;

    private Long projectId;

    @Column(nullable = false, length = 40)
    private String mode;

    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;

    @Column(nullable = false)
    private Double finalScore;

    @Column(length = 80)
    private String reasonCode;

    @Column(length = 40)
    private String policyProfile;

    private Double tieBreakerWeight;

    @Column(nullable = false)
    private Boolean priorityMarkupApplied = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
