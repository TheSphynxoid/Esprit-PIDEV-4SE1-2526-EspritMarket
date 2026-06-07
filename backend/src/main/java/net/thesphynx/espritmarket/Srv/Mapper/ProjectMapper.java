package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Srv.Dto.ProjectMemberSummary;
import net.thesphynx.espritmarket.Srv.Dto.ProjectRequest;
import net.thesphynx.espritmarket.Srv.Dto.ProjectResponse;
import net.thesphynx.espritmarket.Srv.Dto.ProjectServiceSummary;
import net.thesphynx.espritmarket.Srv.Entity.Project;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProjectMapper {
    public Project toEntity(ProjectRequest request) {
        if (request == null) return null;
        Project project = new Project();
        project.setTitle(request.getTitle());
        project.setDetails(request.getDetails());
        project.setStartDate(request.getStartDate());
        project.setEstimatedEndDate(request.getEstimatedEndDate());
        project.setEndDate(request.getEndDate());
        project.setBudget(request.getBudget());
        project.setStatus(request.getStatus());
        project.setPriority(request.getPriority());
        return project;
    }

    public ProjectResponse toResponse(Project project) {
        if (project == null) return null;
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setTitle(project.getTitle());
        response.setDetails(project.getDetails());
        response.setStartDate(project.getStartDate());
        response.setEstimatedEndDate(project.getEstimatedEndDate());
        response.setEndDate(project.getEndDate());
        response.setBudget(project.getBudget());
        response.setStatus(project.getStatus());
        response.setPriority(project.getPriority());
        if (project.getCreator() != null) {
            response.setCreatorId(project.getCreator().getId());
            response.setCreatorName(project.getCreator().getName());
        }
        if (project.getMembers() != null) {
            response.setMembers(project.getMembers().stream()
                    .map(this::toMemberSummary)
                    .toList());
        }
        if (project.getServices() != null) {
            response.setServices(project.getServices().stream()
                    .map(this::toServiceSummary)
                    .toList());
        }
        return response;
    }

    private ProjectMemberSummary toMemberSummary(User user) {
        ProjectMemberSummary s = new ProjectMemberSummary();
        s.setId(user.getId());
        s.setName(user.getName());
        s.setEmail(user.getEmail());
        return s;
    }

    private ProjectServiceSummary toServiceSummary(net.thesphynx.espritmarket.Srv.Entity.Service service) {
        ProjectServiceSummary s = new ProjectServiceSummary();
        s.setId(service.getId());
        s.setName(service.getName());
        s.setCategory(service.getCategory());
        return s;
    }
}
