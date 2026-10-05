package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.util.List;

@Data
public class SlotSuggestionResponse {
    private Long serviceId;
    private Long projectId;
    private SlotScoringMode mode;
    private List<ScoredTimeSlotDto> suggestions;
}
