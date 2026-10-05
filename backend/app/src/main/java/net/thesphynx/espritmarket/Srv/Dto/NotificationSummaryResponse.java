package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationSummaryResponse {
    private long unreadCount;
    private long totalCount;
}
