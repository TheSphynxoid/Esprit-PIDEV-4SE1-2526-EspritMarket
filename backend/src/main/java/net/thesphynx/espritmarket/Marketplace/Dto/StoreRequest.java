package net.thesphynx.espritmarket.Marketplace.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class StoreRequest {
    @NotBlank(message = "Store name is required")
    @Size(min = 3, message = "Store name must have at least 3 characters")
    private String name;
    
    @NotBlank(message = "Store description is required")
    @Size(min = 10, message = "Description must contain at least 10 characters")
    private String description;

    @NotBlank(message = "Store address is required")
    @Size(min = 3, message = "Store address must have at least 3 characters")
    private String address;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9\\s]{8,16}$", message = "Use only digits/spaces, optional +, length 8-16")
    private String phone;

    // Logo URL peut être null - géré séparément via multipart ou profil public
    private String logoUrl;

    private List<String> categories; // List of category names
}
