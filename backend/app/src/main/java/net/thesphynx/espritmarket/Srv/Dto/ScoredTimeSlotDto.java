package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

@Data
public class ScoredTimeSlotDto {
    private TimeSlotDto slot;
    private SlotScoreBreakdownDto score;
    private int rank;
}
