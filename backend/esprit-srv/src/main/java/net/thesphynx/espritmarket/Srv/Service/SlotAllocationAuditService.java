package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Srv.Dto.SlotAllocationAuditResponse;
import net.thesphynx.espritmarket.Srv.Dto.SlotScoringMode;
import net.thesphynx.espritmarket.Srv.Dto.SlotSuggestionResponse;
import net.thesphynx.espritmarket.Srv.Entity.SlotAllocationAudit;
import net.thesphynx.espritmarket.Srv.Mapper.SlotAllocationAuditMapper;
import net.thesphynx.espritmarket.Srv.Repository.ISlotAllocationAuditRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SlotAllocationAuditService {

    private final ISlotAllocationAuditRepository auditRepository;
    private final SlotAllocationAuditMapper mapper;

    public SlotAllocationAuditService(ISlotAllocationAuditRepository auditRepository,
                                      SlotAllocationAuditMapper mapper) {
        this.auditRepository = auditRepository;
        this.mapper = mapper;
    }

    public void recordTopSuggestions(SlotSuggestionResponse suggestionResponse) {
        if (suggestionResponse == null || suggestionResponse.getSuggestions() == null) {
            return;
        }

        List<SlotAllocationAudit> audits = new ArrayList<>();

        suggestionResponse.getSuggestions().stream()
                .limit(10)
                .forEach(suggestion -> {
                    if (suggestion.getSlot() == null || suggestion.getScore() == null) {
                        return;
                    }

                    SlotAllocationAudit audit = new SlotAllocationAudit();
                    audit.setServiceId(suggestionResponse.getServiceId());
                    audit.setProjectId(suggestionResponse.getProjectId());
                    audit.setMode(suggestionResponse.getMode() != null ? suggestionResponse.getMode().name() : null);
                    audit.setSlotStart(suggestion.getSlot().getStart());
                    audit.setSlotEnd(suggestion.getSlot().getEnd());
                    audit.setFinalScore(suggestion.getScore().getFinalScore());
                    audit.setReasonCode(suggestion.getScore().getReasonCode());
                    audit.setPolicyProfile(suggestion.getScore().getPolicyProfile());
                    audit.setTieBreakerWeight(suggestion.getScore().getTieBreakerWeight());
                    audit.setPriorityMarkupApplied(
                            suggestionResponse.getMode() == SlotScoringMode.PROJECT_FIRST);
                    audits.add(audit);
                });

        if (!audits.isEmpty()) {
            auditRepository.saveAll(audits);
        }
    }

    public List<SlotAllocationAuditResponse> getByProjectId(Long projectId) {
        return auditRepository.findTop50ByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<SlotAllocationAuditResponse> getByServiceId(Long serviceId) {
        return auditRepository.findTop50ByServiceIdOrderByCreatedAtDesc(serviceId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<SlotAllocationAuditResponse> getRecent(int limit) {
        return auditRepository.findAll(org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
