package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MilestoneReorderRequest {
    @NotNull(message = "Ordered milestone IDs are required")
    private List<Long> orderedMilestoneIds;
}
