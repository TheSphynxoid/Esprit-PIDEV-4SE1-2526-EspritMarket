package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DisputeMediationResponse {
    private Long id;
    private Long bookingId;
    private String suggestedResolution;
    private Double suggestedRefundPercent;
    private Integer suggestedDeadlineExtensionDays;
    private String analysisSummary;
    private String clientVote;
    private String providerVote;
    private String status;
    private String resolvedAt;
    private String createdAt;
}
