package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.ProjectStatus;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ProjectRequest {
    @NotBlank(message = "Project title is required")
    @Size(min = 3, max = 150, message = "Project title must be between 3 and 150 characters")
    private String title;

    @Size(max = 1000, message = "Project details must not exceed 1000 characters")
    private String details;

    @NotNull(message = "Start date is required")
    private Date startDate;

    @NotNull(message = "Estimated end date is required")
    private Date estimatedEndDate;

    private Date endDate;

    @DecimalMin(value = "0.0", message = "Budget must be a positive value or zero")
    private BigDecimal budget;

    @NotNull(message = "Project status is required")
    private ProjectStatus status;

    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "Priority must be one of: LOW, MEDIUM, HIGH, CRITICAL")
    private String priority;
}
