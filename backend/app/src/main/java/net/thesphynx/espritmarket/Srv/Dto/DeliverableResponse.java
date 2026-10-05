package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.DeliverableStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeliverableResponse {
    private Long id;
    private Long bookingId;
    private Long providerId;
    private String providerName;
    private String title;
    private String description;
    private DeliverableStatus status;
    private int version;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DeliverableAttachmentResponse> attachments;
    private List<DeliverableReviewResponse> reviews;
    private Integer qualityScore;
    private List<String> qualityWarnings;
}
