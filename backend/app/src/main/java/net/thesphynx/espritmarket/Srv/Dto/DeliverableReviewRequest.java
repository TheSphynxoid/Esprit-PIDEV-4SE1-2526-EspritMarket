package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.ReviewDecision;

@Data
public class DeliverableReviewRequest {
    @NotNull(message = "Decision is required")
    private ReviewDecision decision;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;
}
