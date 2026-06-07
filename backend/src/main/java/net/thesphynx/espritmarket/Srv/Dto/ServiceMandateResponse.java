package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

@Data
public class ServiceMandateResponse {
    private Long id;
    private Long providerId;
    private String providerName;
    private Long serviceId;
    private String serviceName;
    private int maxBookings;
    private int currentBookings;
    private boolean overbooked;
}
