package net.thesphynx.espritmarket.Delivery.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryCourierContactResponse {
    private Long courierId;
    private Long userId;
    private String name;
    private String email;
    private String phoneNumber;
}
