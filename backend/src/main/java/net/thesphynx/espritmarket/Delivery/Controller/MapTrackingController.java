package net.thesphynx.espritmarket.Delivery.Controller;

import net.thesphynx.espritmarket.Delivery.Dto.MapTrackingDto;
import net.thesphynx.espritmarket.Delivery.Entity.MapTracking;
import net.thesphynx.espritmarket.Delivery.Service.MapTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery/map-tracking")
@Tag(name = "Delivery - Map Tracking")
public class MapTrackingController {
    private final MapTrackingService mapTrackingService;

    public MapTrackingController(MapTrackingService mapTrackingService) {
        this.mapTrackingService = mapTrackingService;
    }

    @GetMapping
    @Operation(summary = "List map tracking records")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Map tracking records retrieved")})
    public List<MapTracking> getAll() {
        return mapTrackingService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get map tracking record by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Map tracking record found"),
        @ApiResponse(responseCode = "404", description = "Map tracking record not found")
    })
    public ResponseEntity<MapTracking> getById(@PathVariable Long id) {
        return mapTrackingService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create map tracking record")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Map tracking record created")})
    public MapTracking create(@Valid @RequestBody MapTrackingDto request) {
        return mapTrackingService.create(toEntity(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update map tracking record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Map tracking record updated"),
        @ApiResponse(responseCode = "404", description = "Map tracking record not found")
    })
    public ResponseEntity<MapTracking> update(@PathVariable Long id, @Valid @RequestBody MapTrackingDto request) {
        if (mapTrackingService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapTrackingService.update(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete map tracking record")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Map tracking record deleted"),
        @ApiResponse(responseCode = "404", description = "Map tracking record not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (mapTrackingService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        mapTrackingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private MapTracking toEntity(MapTrackingDto request) {
        MapTracking tracking = new MapTracking();
        tracking.setCurrentLocation(request.getCurrentLocation());
        tracking.setLastUpdate(request.getLastUpdate());
        tracking.setEstimatedArrival(request.getEstimatedArrival());
        return tracking;
    }
}
