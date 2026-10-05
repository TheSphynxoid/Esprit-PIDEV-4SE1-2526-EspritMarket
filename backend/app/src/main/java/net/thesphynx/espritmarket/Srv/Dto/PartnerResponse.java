package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Common.Entity.Role;

@Data
public class PartnerResponse {
    private Long id;
    private String name;
    private String contactInfo;
    private Role role;
}
