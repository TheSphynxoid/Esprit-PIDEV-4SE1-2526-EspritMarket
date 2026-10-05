package net.thesphynx.espritmarket.Partnership.Dto;

import lombok.Data;
import java.util.List;

@Data
public class ProfilRequest {
    private List<String> skills;
    private String experienceLevel;
    private String fieldOfStudy;
    private String yearsOfExperience;
    private List<String> languages;
}
