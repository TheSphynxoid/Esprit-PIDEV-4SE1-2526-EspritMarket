package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SlotAllocationAuditResponse {
    private Long id;
    private Long serviceId;
    private Long projectId;
    private String mode;
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;
    private Double finalScore;
    private String reasonCode;
    private String policyProfile;
    private Double tieBreakerWeight;
    private Boolean priorityMarkupApplied;
    private LocalDateTime createdAt;
}
