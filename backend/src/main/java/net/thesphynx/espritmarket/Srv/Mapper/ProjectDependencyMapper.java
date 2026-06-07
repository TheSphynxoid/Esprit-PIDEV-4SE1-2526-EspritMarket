package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.ProjectDependencyResponse;
import net.thesphynx.espritmarket.Srv.Entity.ProjectDependency;
import org.springframework.stereotype.Component;

@Component
public class ProjectDependencyMapper {

    public ProjectDependencyResponse toResponse(ProjectDependency dependency) {
        if (dependency == null) return null;

        ProjectDependencyResponse response = new ProjectDependencyResponse();
        response.setId(dependency.getId());
        if (dependency.getProject() != null) {
            response.setProjectId(dependency.getProject().getId());
        }
        if (dependency.getPredecessorMilestone() != null) {
            response.setPredecessorMilestoneId(dependency.getPredecessorMilestone().getId());
            response.setPredecessorMilestoneTitle(dependency.getPredecessorMilestone().getTitle());
        }
        if (dependency.getSuccessorMilestone() != null) {
            response.setSuccessorMilestoneId(dependency.getSuccessorMilestone().getId());
            response.setSuccessorMilestoneTitle(dependency.getSuccessorMilestone().getTitle());
        }
        response.setCreatedAt(dependency.getCreatedAt());
        return response;
    }
}
