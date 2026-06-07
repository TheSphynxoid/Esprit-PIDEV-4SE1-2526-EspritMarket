package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TimeTrackingResponse {
    private Long bookingId;
    private List<TimeLogEntry> entries;
    private long totalMinutes;
    private boolean hasActiveTimer;
    private TimeLogEntry activeTimer;
    private long estimatedMinutes;

    @Data
    @Builder
    public static class TimeLogEntry {
        private Long id;
        private String startTime;
        private String endTime;
        private Integer durationMinutes;
        private String description;
        private boolean active;
    }
}
