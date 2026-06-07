package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.ProjectStatus;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ProjectResponse {
    private Long id;
    private String title;
    private String details;
    private Date startDate;
    private Date estimatedEndDate;
    private Date endDate;
    private BigDecimal budget;
    private ProjectStatus status;
    private String priority;
    private Long creatorId;
    private String creatorName;
    private List<ProjectMemberSummary> members;
    private List<ProjectServiceSummary> services;
}
