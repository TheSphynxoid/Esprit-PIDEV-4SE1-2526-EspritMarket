package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.MilestoneType;
import net.thesphynx.espritmarket.Srv.Entity.ProjectMilestoneStatus;

import java.util.Date;

@Data
public class ProjectMilestoneRequest {
    @NotBlank(message = "Milestone title is required")
    @Size(max = 150, message = "Milestone title must not exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "Milestone details must not exceed 2000 characters")
    private String details;

    private Date plannedStartDate;
    private Date plannedEndDate;
    private Date actualStartDate;
    private Date actualEndDate;

    @NotNull(message = "Milestone status is required")
    private ProjectMilestoneStatus status;

    private MilestoneType milestoneType;
    private String conditionExpression;
    private Long assignedProviderId;
    private String handoffNotes;

    @NotNull(message = "Sort order is required")
    @PositiveOrZero(message = "Sort order must be zero or positive")
    private Integer sortOrder;
}
