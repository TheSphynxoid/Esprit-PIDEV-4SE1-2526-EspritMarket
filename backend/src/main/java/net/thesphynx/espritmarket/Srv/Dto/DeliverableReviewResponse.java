package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.ReviewDecision;

import java.time.LocalDateTime;

@Data
public class DeliverableReviewResponse {
    private Long id;
    private Long deliverableId;
    private Long reviewerId;
    private String reviewerName;
    private ReviewDecision decision;
    private String comment;
    private LocalDateTime reviewedAt;
}
