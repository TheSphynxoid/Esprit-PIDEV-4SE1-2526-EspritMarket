package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

@Data
public class ProviderMandateResponse {
    private Long id;
    private Long providerId;
    private String providerName;
    private int maxBookings;
    private int currentBookings;
    private boolean overbooked;
}
