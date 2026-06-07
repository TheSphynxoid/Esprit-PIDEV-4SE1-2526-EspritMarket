package net.thesphynx.espritmarket.Delivery.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.thesphynx.espritmarket.Delivery.Entity.CourierProfileStatus;
import net.thesphynx.espritmarket.Delivery.Entity.CourierStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierProfileResponse {
    private Long id;
    private Long userId;
    private String phoneNumber;
    private String permitFileName;
    private String permitContentType;
    private boolean permitImageUploaded;
    private LocalDateTime interviewDate;
    private CourierStatus status;
    private CourierProfileStatus profileStatus;
}
