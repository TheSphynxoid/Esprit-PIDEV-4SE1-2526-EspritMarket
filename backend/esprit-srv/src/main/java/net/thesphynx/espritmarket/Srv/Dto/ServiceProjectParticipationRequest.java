package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceProjectParticipationRequest {
    @NotNull(message = "allowProjectParticipation is required")
    private Boolean allowProjectParticipation;
}
