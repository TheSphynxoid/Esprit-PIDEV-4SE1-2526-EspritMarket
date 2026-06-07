package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ProjectTimelineResponse {
    private Long projectId;
    private String projectTitle;
    private List<ProjectMilestoneResponse> milestones;
    private List<ProjectDependencyResponse> dependencies;
    private Integer totalMilestones;
    private Integer completedMilestones;
    private Integer blockedMilestones;
    private Double completionPercent;
}
