package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.DeliverableResponse;
import net.thesphynx.espritmarket.Srv.Entity.Deliverable;
import org.springframework.stereotype.Component;

@Component
public class DeliverableMapper {
    private final DeliverableAttachmentMapper attachmentMapper;
    private final DeliverableReviewMapper reviewMapper;

    public DeliverableMapper(DeliverableAttachmentMapper attachmentMapper,
                             DeliverableReviewMapper reviewMapper) {
        this.attachmentMapper = attachmentMapper;
        this.reviewMapper = reviewMapper;
    }

    public DeliverableResponse toResponse(Deliverable deliverable) {
        if (deliverable == null) return null;
        DeliverableResponse response = new DeliverableResponse();
        response.setId(deliverable.getId());
        if (deliverable.getBooking() != null) {
            response.setBookingId(deliverable.getBooking().getId());
        }
        if (deliverable.getProvider() != null) {
            response.setProviderId(deliverable.getProvider().getId());
            response.setProviderName(deliverable.getProvider().getName());
        }
        response.setTitle(deliverable.getTitle());
        response.setDescription(deliverable.getDescription());
        response.setStatus(deliverable.getStatus());
        response.setVersion(deliverable.getVersion());
        response.setSubmittedAt(deliverable.getSubmittedAt());
        response.setReviewedAt(deliverable.getReviewedAt());
        response.setCreatedAt(deliverable.getCreatedAt());
        response.setUpdatedAt(deliverable.getUpdatedAt());
        if (deliverable.getAttachments() != null) {
            response.setAttachments(deliverable.getAttachments().stream()
                    .map(attachmentMapper::toResponse).toList());
        }
        if (deliverable.getReviews() != null) {
            response.setReviews(deliverable.getReviews().stream()
                    .map(reviewMapper::toResponse).toList());
        }
        return response;
    }

    public DeliverableResponse toSummaryResponse(Deliverable deliverable) {
        if (deliverable == null) return null;
        DeliverableResponse response = new DeliverableResponse();
        response.setId(deliverable.getId());
        if (deliverable.getBooking() != null) {
            response.setBookingId(deliverable.getBooking().getId());
        }
        if (deliverable.getProvider() != null) {
            response.setProviderId(deliverable.getProvider().getId());
            response.setProviderName(deliverable.getProvider().getName());
        }
        response.setTitle(deliverable.getTitle());
        response.setDescription(deliverable.getDescription());
        response.setStatus(deliverable.getStatus());
        response.setVersion(deliverable.getVersion());
        response.setSubmittedAt(deliverable.getSubmittedAt());
        response.setReviewedAt(deliverable.getReviewedAt());
        response.setCreatedAt(deliverable.getCreatedAt());
        response.setUpdatedAt(deliverable.getUpdatedAt());
        return response;
    }
}
