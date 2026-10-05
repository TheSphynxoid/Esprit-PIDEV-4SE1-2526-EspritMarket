package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.ServiceCategory;

@Data
public class ProjectServiceSummary {
    private Long id;
    private String name;
    private ServiceCategory category;
}
