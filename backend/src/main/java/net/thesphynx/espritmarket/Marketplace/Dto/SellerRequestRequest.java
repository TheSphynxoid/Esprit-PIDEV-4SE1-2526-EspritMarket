package net.thesphynx.espritmarket.Marketplace.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SellerRequestRequest {

    @NotBlank(message = "Student number is required")
    @Size(max = 50, message = "Student number must not exceed 50 characters")
    private String numeroEtudiant;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String prenom;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String nom;

    @jakarta.validation.constraints.NotBlank(message = "Email is required")
    @jakarta.validation.constraints.Email(message = "Email format is invalid")
    @jakarta.validation.constraints.Pattern(regexp = "^.+@esprit\\.tn$", message = "Email must end with @esprit.tn")
    @jakarta.validation.constraints.Size(max = 320, message = "Email must not exceed 320 characters")
    private String email;

    @NotBlank(message = "Student card URL is required")
    @Size(max = 500, message = "Student card URL must not exceed 500 characters")
    private String carteEtudiantUrl;
}
