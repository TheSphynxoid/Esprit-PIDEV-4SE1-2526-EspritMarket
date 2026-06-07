package net.thesphynx.espritmarket.Delivery.Dto;

public class GoogleMapsConfigDto {
    private final boolean configured;
    private final String baseUrl;

    public GoogleMapsConfigDto(boolean configured, String baseUrl) {
        this.configured = configured;
        this.baseUrl = baseUrl;
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

}
