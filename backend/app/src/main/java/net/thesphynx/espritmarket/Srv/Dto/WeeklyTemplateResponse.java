package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class WeeklyTemplateResponse {
    private Long id;
    private Long providerId;
    private String providerName;
    private Long serviceId;
    private String serviceName;
    private DayOfWeek dayOfWeek;
    private LocalTime startHour;
    private LocalTime endHour;
    private int slotDurationMinutes;
    private int maxConcurrent;
}
