package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

@Data
public class SlotScoreBreakdownDto {
    private String policyProfile;
    private double availabilityWeight;
    private double scarcityWeight;
    private double projectUrgencyWeight;
    private double projectProgressWeight;
    private double reliabilityWeight;
    private double fairnessWeight;
    private double tieBreakerWeight;
    private double modeMultiplier;
    private double finalScore;
    private String reasonCode;
}
