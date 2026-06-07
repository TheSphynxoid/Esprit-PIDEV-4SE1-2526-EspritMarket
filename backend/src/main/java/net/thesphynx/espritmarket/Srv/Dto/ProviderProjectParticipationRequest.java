package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProviderProjectParticipationRequest {
    @NotNull(message = "Provider ID is required")
    @Positive(message = "Provider ID must be positive")
    private Long providerId;

    @NotNull(message = "allowProjectParticipation is required")
    private Boolean allowProjectParticipation;
}
