package net.thesphynx.espritmarket.Partnership.Dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AiPredictRequest {
    private List<String> cv_skills;
    private String cv_experience_level;
    private String cv_field_of_study;
    private String cv_years_of_experience;
    private List<String> cv_languages;
    private List<String> job_required_skills;
    private String job_experience_level;
}
