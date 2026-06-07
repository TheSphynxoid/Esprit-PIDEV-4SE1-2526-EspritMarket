package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.DeliverableReviewResponse;
import net.thesphynx.espritmarket.Srv.Entity.DeliverableReview;
import org.springframework.stereotype.Component;

@Component
public class DeliverableReviewMapper {
    public DeliverableReviewResponse toResponse(DeliverableReview review) {
        if (review == null) return null;
        DeliverableReviewResponse response = new DeliverableReviewResponse();
        response.setId(review.getId());
        if (review.getDeliverable() != null) {
            response.setDeliverableId(review.getDeliverable().getId());
        }
        if (review.getReviewer() != null) {
            response.setReviewerId(review.getReviewer().getId());
            response.setReviewerName(review.getReviewer().getName());
        }
        response.setDecision(review.getDecision());
        response.setComment(review.getComment());
        response.setReviewedAt(review.getReviewedAt());
        return response;
    }
}
