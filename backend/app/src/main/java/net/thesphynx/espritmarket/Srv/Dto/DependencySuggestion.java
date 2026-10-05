package net.thesphynx.espritmarket.Srv.Dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class DependencySuggestion {
    private Long predecessorMilestoneId;
    private String predecessorMilestoneTitle;
    private Long successorMilestoneId;
    private String successorMilestoneTitle;
    private String reason;
    private Double confidence;

    @JsonIgnore
    private Integer distance;
}
