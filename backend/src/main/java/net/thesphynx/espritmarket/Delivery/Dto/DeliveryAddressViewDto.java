package net.thesphynx.espritmarket.Delivery.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddressViewDto {
    private String deliveryAddress;
    private String city;
    private String postalCode;
    private String phoneNumber;
}
