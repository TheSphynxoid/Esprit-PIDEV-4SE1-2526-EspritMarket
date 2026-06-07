package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.SlotAllocationAuditResponse;
import net.thesphynx.espritmarket.Srv.Entity.SlotAllocationAudit;
import org.springframework.stereotype.Component;

@Component
public class SlotAllocationAuditMapper {
    public SlotAllocationAuditResponse toResponse(SlotAllocationAudit audit) {
        if (audit == null) return null;

        SlotAllocationAuditResponse response = new SlotAllocationAuditResponse();
        response.setId(audit.getId());
        response.setServiceId(audit.getServiceId());
        response.setProjectId(audit.getProjectId());
        response.setMode(audit.getMode());
        response.setSlotStart(audit.getSlotStart());
        response.setSlotEnd(audit.getSlotEnd());
        response.setFinalScore(audit.getFinalScore());
        response.setReasonCode(audit.getReasonCode());
        response.setPolicyProfile(audit.getPolicyProfile());
        response.setTieBreakerWeight(audit.getTieBreakerWeight());
        response.setPriorityMarkupApplied(audit.getPriorityMarkupApplied());
        response.setCreatedAt(audit.getCreatedAt());
        return response;
    }
}
