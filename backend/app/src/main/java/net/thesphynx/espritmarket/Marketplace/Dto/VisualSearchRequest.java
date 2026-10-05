package net.thesphynx.espritmarket.Marketplace.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisualSearchRequest {

    // ✅ @JsonProperty garantit le mapping même si Jackson est strict
    @JsonProperty("imageBase64")
    @NotBlank(message = "imageBase64 is required")
    // ✅ Limite 10 MB en base64 (~7.5 MB image réelle)
    @Size(max = 10_000_000, message = "Image trop volumineuse (max ~7 MB)")
    private String imageBase64;
}