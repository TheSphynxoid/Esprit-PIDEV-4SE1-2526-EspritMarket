package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.ProjectMilestoneRequest;
import net.thesphynx.espritmarket.Srv.Dto.ProjectMilestoneResponse;
import net.thesphynx.espritmarket.Srv.Entity.MilestoneType;
import net.thesphynx.espritmarket.Srv.Entity.Project;
import net.thesphynx.espritmarket.Srv.Entity.ProjectMilestone;
import net.thesphynx.espritmarket.Srv.Entity.Service;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ProjectMilestoneMapper {
    private final UserRepository userRepository;

    public ProjectMilestoneMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public ProjectMilestone toEntity(ProjectMilestoneRequest request, Project project) {
        if (request == null || project == null) return null;

        ProjectMilestone milestone = new ProjectMilestone();
        milestone.setProject(project);
        milestone.setOriginalTitle(request.getTitle());
        milestone.setOriginalDetails(request.getDetails());
        applyRequest(milestone, request);
        return milestone;
    }

    public void applyRequest(ProjectMilestone milestone, ProjectMilestoneRequest request) {
        milestone.setTitle(request.getTitle());
        milestone.setDetails(request.getDetails());
        milestone.setPlannedStartDate(request.getPlannedStartDate());
        milestone.setPlannedEndDate(request.getPlannedEndDate());
        milestone.setActualStartDate(request.getActualStartDate());
        milestone.setActualEndDate(request.getActualEndDate());
        milestone.setStatus(request.getStatus());
        milestone.setMilestoneType(request.getMilestoneType() != null ? request.getMilestoneType() : MilestoneType.MILESTONE);
        milestone.setConditionExpression(request.getConditionExpression());
        milestone.setSortOrder(request.getSortOrder());
        milestone.setAssignedProviderId(request.getAssignedProviderId());
        milestone.setHandoffNotes(request.getHandoffNotes());
    }

    public ProjectMilestoneResponse toResponse(ProjectMilestone milestone) {
        if (milestone == null) return null;

        ProjectMilestoneResponse response = new ProjectMilestoneResponse();
        response.setId(milestone.getId());
        if (milestone.getProject() != null) {
            response.setProjectId(milestone.getProject().getId());
        }
        response.setTitle(milestone.getTitle());
        response.setDetails(milestone.getDetails());
        response.setPlannedStartDate(milestone.getPlannedStartDate());
        response.setPlannedEndDate(milestone.getPlannedEndDate());
        response.setActualStartDate(milestone.getActualStartDate());
        response.setActualEndDate(milestone.getActualEndDate());
        response.setStatus(milestone.getStatus());
        response.setMilestoneType(milestone.getMilestoneType());
        response.setConditionExpression(milestone.getConditionExpression());
        response.setSortOrder(milestone.getSortOrder());
        response.setAssignedProviderId(milestone.getAssignedProviderId());
        response.setHandoffNotes(milestone.getHandoffNotes());
        if (milestone.getAssignedProviderId() != null && milestone.getProject() != null) {
            response.setAssignedProviderName(resolveProviderName(milestone.getAssignedProviderId()));
        }
        response.setCreatedAt(milestone.getCreatedAt());
        response.setUpdatedAt(milestone.getUpdatedAt());
        if (milestone.getBookings() != null) {
            response.setLinkedBookingIds(milestone.getBookings().stream()
                    .map(net.thesphynx.espritmarket.Srv.Entity.Booking::getId)
                    .toList());
        }
        if (milestone.getServices() != null) {
            response.setServices(milestone.getServices().stream()
                    .sorted(Comparator.comparing(Service::getId))
                    .map(this::toServiceSummary)
                    .toList());
        } else {
            response.setServices(List.of());
        }
        return response;
    }

    private ProjectMilestoneResponse.MilestoneServiceSummary toServiceSummary(Service service) {
        ProjectMilestoneResponse.MilestoneServiceSummary summary = new ProjectMilestoneResponse.MilestoneServiceSummary();
        summary.setId(service.getId());
        summary.setName(service.getName());
        summary.setCategory(service.getCategory() != null ? service.getCategory().name() : null);
        summary.setPrice(service.getPrice());
        summary.setPricingType(service.getPricingType() != null ? service.getPricingType().name() : null);
        if (service.getProvider() != null) {
            summary.setProviderId(service.getProvider().getId());
            summary.setProviderName(service.getProvider().getName());
        }
        return summary;
    }

    private String resolveProviderName(Long providerId) {
        return userRepository.findById(providerId)
                .map(u -> u.getName())
                .orElse("Unknown");
    }
}
