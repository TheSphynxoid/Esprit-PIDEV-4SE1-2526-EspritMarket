package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.thesphynx.espritmarket.Common.Entity.Role;

@Data
public class PartnerRequest {
    @NotBlank(message = "Partner name is required")
    @Size(min = 2, max = 100, message = "Partner name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Contact information is required")
    @Size(min = 5, max = 200, message = "Contact information must be between 5 and 200 characters")
    @Pattern(regexp = "^[\\w\\s@.+\\-()]+$", message = "Contact information contains invalid characters")
    private String contactInfo;

    @NotNull(message = "Role is required")
    private Role role;
}
