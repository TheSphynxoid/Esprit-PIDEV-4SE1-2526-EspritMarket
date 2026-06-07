package net.thesphynx.espritmarket.EventPlanning.Controller;

import net.thesphynx.espritmarket.EventPlanning.Dto.ReservationRequest;
import net.thesphynx.espritmarket.EventPlanning.Dto.ReservationWithEquipmentRequest;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import net.thesphynx.espritmarket.EventPlanning.Entity.Reservation;
import net.thesphynx.espritmarket.EventPlanning.Service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventplanning/reservations")
@Tag(name = "EventPlanning - Reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @Operation(summary = "List reservations")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Reservations retrieved")})
    public List<Reservation> getAll() {
        return reservationService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation found"),
        @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<Reservation> getById(@PathVariable Long id) {
        return reservationService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create reservation")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Reservation created")})
    public Reservation create(@Valid @RequestBody ReservationRequest request) {
        return reservationService.create(toEntity(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update reservation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation updated"),
        @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<Reservation> update(@PathVariable Long id, @Valid @RequestBody ReservationRequest request) {
        if (reservationService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reservationService.update(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete reservation")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reservation deleted"),
        @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (reservationService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/with-equipment")
    @Operation(summary = "Create reservation with equipment and quantities")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Reservation with equipment created")})
    public Reservation createWithEquipment(@Valid @RequestBody ReservationWithEquipmentRequest request) {
        return reservationService.createWithEquipment(request);
    }

    private Reservation toEntity(ReservationRequest request) {
        Reservation reservation = new Reservation();
        reservation.setName(request.getName());
        reservation.setDate(request.getDate());

        Event event = new Event();
        event.setId(request.getEventId());
        reservation.setEvent(event);

        return reservation;
    }
}
