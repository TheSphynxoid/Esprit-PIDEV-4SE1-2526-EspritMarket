package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.ProviderExceptionType;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ProviderExceptionResponse {
    private Long id;
    private Long providerId;
    private String providerName;
    private LocalDate date;
    private ProviderExceptionType type;
    private LocalTime startHour;
    private LocalTime endHour;
    private String reason;
}
