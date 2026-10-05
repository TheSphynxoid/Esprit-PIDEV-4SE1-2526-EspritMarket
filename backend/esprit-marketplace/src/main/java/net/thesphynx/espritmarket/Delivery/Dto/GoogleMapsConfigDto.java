package net.thesphynx.espritmarket.Delivery.Dto;

public class GoogleMapsConfigDto {
    private final boolean configured;
    private final String baseUrl;
    private final String apiKey;

    public GoogleMapsConfigDto(boolean configured, String baseUrl, String apiKey) {
        this.configured = configured;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }
}
