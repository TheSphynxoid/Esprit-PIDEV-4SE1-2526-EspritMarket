package net.thesphynx.espritmarket.Partnership.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartnerCompanyRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String name;

    @Size(max = 100, message = "Sector must not exceed 100 characters")
    private String sector;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String contactEmail;

    @NotBlank(message = "Partnership status is required")
    @Pattern(regexp = "PENDING|APPROVED|REJECTED",
            message = "Status must be PENDING, APPROVED, or REJECTED")
    private String partnershipStatus;
}
