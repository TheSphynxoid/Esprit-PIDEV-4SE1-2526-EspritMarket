package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectDependencyResponse {
    private Long id;
    private Long projectId;
    private Long predecessorMilestoneId;
    private String predecessorMilestoneTitle;
    private Long successorMilestoneId;
    private String successorMilestoneTitle;
    private LocalDateTime createdAt;
}
