package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProjectDependencyRequest {
    @NotNull(message = "Predecessor milestone id is required")
    @Positive(message = "Predecessor milestone id must be positive")
    private Long predecessorMilestoneId;

    @NotNull(message = "Successor milestone id is required")
    @Positive(message = "Successor milestone id must be positive")
    private Long successorMilestoneId;
}
