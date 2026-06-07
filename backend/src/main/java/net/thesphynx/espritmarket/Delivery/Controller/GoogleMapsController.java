package net.thesphynx.espritmarket.Delivery.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.thesphynx.espritmarket.Delivery.Config.GoogleMapsProperties;
import net.thesphynx.espritmarket.Delivery.Dto.GoogleMapsConfigDto;
import net.thesphynx.espritmarket.Delivery.Service.GoogleMapsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery/maps")
@Tag(name = "Delivery - Google Maps")
public class GoogleMapsController {

    private final GoogleMapsProperties googleMapsProperties;
    private final GoogleMapsService googleMapsService;

    public GoogleMapsController(GoogleMapsProperties googleMapsProperties, GoogleMapsService googleMapsService) {
        this.googleMapsProperties = googleMapsProperties;
        this.googleMapsService = googleMapsService;
    }

    @GetMapping("/config")
    @Operation(summary = "Get Google Maps backend configuration status")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Google Maps configuration status returned")})
    public GoogleMapsConfigDto getConfigStatus() {
        return new GoogleMapsConfigDto(
                googleMapsProperties.isConfigured(),
                googleMapsProperties.getBaseUrl()
        );
    }
//aficher ligne dans google map

    @GetMapping("/distance")
    @Operation(summary = "Get travel distance and duration between two points")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Distance matrix response returned")})
    public String getDistance(
            @RequestParam double lat1,
            @RequestParam double lng1,
            @RequestParam double lat2,
            @RequestParam double lng2) {

        return googleMapsService.getDistance(lat1, lng1, lat2, lng2);
    }

    @GetMapping("/route")
    @Operation(summary = "Get route details between two points for map drawing")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Directions response returned")})
    public String getRoute(
            @RequestParam double lat1,
            @RequestParam double lng1,
            @RequestParam double lat2,
            @RequestParam double lng2) {

        return googleMapsService.getRoute(lat1, lng1, lat2, lng2);
    }
}
