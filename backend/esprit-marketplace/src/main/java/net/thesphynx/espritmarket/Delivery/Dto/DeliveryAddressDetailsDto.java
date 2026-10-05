package net.thesphynx.espritmarket.Delivery.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddressDetailsDto {

    @NotBlank(message = "deliveryAddress is required")
    private String deliveryAddress;

    @NotBlank(message = "city is required")
    private String city;

    @NotBlank(message = "Le code postal est obligatoire")
    @Pattern(regexp = "^[0-9]{4}$", message = "Le code postal doit contenir exactement 4 chiffres")
    private String postalCode;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(
            regexp = "^(\\+216)?[0-9]{8}$",
            message = "Le numéro doit contenir 8 chiffres (optionnellement +216)"
    )
    private String phoneNumber;

    private Long deliveryId;
}
