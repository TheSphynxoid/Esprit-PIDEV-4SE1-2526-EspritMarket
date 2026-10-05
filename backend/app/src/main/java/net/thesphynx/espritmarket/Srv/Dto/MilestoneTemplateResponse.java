package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.util.List;

@Data
public class MilestoneTemplateResponse {
    private String title;
    private String details;
    private Integer sortOrder;
    private String category;

    @Data
    public static class TemplateSet {
        private List<MilestoneTemplateResponse> milestones;
        private List<DependencySuggestion> suggestedDependencies;
    }
}
