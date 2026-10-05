package net.thesphynx.espritmarket.Delivery.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.thesphynx.espritmarket.Delivery.Entity.CourierStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCourierInfoResponse {
    private Long courierId;
    private Long userId;
    private String nom;
    private String prenom;
    private String email;
    private String phoneNumber;
    private List<VehicleInfoDto> voitures;
    private String permitImage;
    private LocalDateTime interviewDate;
    private CourierStatus status;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleInfoDto {
        private String serie;
        private String type;
    }
}