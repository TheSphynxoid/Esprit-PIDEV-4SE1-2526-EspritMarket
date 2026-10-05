package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.ProjectStatus;

@Data
public class ProjectStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private ProjectStatus status;

    private String reason;
}
