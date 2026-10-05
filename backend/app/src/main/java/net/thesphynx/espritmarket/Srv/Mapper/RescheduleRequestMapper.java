package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.RescheduleResponse;
import net.thesphynx.espritmarket.Srv.Entity.RescheduleRequest;
import org.springframework.stereotype.Component;

@Component
public class RescheduleRequestMapper {
    public RescheduleResponse toResponse(RescheduleRequest entity) {
        if (entity == null) return null;
        RescheduleResponse response = new RescheduleResponse();
        response.setId(entity.getId());
        if (entity.getBooking() != null) {
            response.setBookingId(entity.getBooking().getId());
        }
        if (entity.getRequestedBy() != null) {
            response.setRequestedById(entity.getRequestedBy().getId());
            response.setRequestedByName(entity.getRequestedBy().getName());
        }
        response.setOriginalDate(entity.getOriginalDate());
        response.setOriginalDuration(entity.getOriginalDuration());
        response.setProposedDate(entity.getProposedDate());
        response.setProposedDuration(entity.getProposedDuration());
        response.setReason(entity.getReason() != null ? entity.getReason().name() : null);
        response.setMessage(entity.getMessage());
        response.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        if (entity.getRespondedBy() != null) {
            response.setRespondedById(entity.getRespondedBy().getId());
            response.setRespondedByName(entity.getRespondedBy().getName());
        }
        response.setRespondedAt(entity.getRespondedAt());
        response.setResponseMessage(entity.getResponseMessage());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
