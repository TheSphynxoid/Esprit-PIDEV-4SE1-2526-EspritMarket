package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Delivery.Config.GoogleMapsProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleMapsService {

    private final GoogleMapsProperties googleMapsProperties;
    private final RestTemplate restTemplate;

    public GoogleMapsService(GoogleMapsProperties googleMapsProperties) {
        this.googleMapsProperties = googleMapsProperties;
        this.restTemplate = new RestTemplate();
    }

    public String getDistance(double lat1, double lng1, double lat2, double lng2) {
        String url = UriComponentsBuilder
                .fromUriString(googleMapsProperties.getBaseUrl() + "/distancematrix/json")
                .queryParam("origins", lat1 + "," + lng1)
                .queryParam("destinations", lat2 + "," + lng2)
                .queryParam("key", googleMapsProperties.getApiKey())
                .toUriString();

        return restTemplate.getForObject(url, String.class);
    }

    public String getRoute(double lat1, double lng1, double lat2, double lng2) {
        String url = UriComponentsBuilder
                .fromUriString(googleMapsProperties.getBaseUrl() + "/directions/json")
                .queryParam("origin", lat1 + "," + lng1)
                .queryParam("destination", lat2 + "," + lng2)
                .queryParam("key", googleMapsProperties.getApiKey())
                .toUriString();

        return restTemplate.getForObject(url, String.class);
    }
}
