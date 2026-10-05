package net.thesphynx.espritmarket.EventPlanning.Controller;

import net.thesphynx.espritmarket.EventPlanning.Dto.EquipmentReservationRequest;
import net.thesphynx.espritmarket.EventPlanning.Entity.Equipment;
import net.thesphynx.espritmarket.EventPlanning.Entity.EquipmentReservation;
import net.thesphynx.espritmarket.EventPlanning.Entity.Reservation;
import net.thesphynx.espritmarket.EventPlanning.Entity.Stall;
import net.thesphynx.espritmarket.EventPlanning.Service.EquipmentReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventplanning/equipment-reservations")
@Tag(name = "EventPlanning - Equipment Reservations")
public class EquipmentReservationController {
    private final EquipmentReservationService equipmentReservationService;

    public EquipmentReservationController(EquipmentReservationService equipmentReservationService) {
        this.equipmentReservationService = equipmentReservationService;
    }

    @GetMapping
    @Operation(summary = "List equipment reservations")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Equipment reservations retrieved")})
    public List<EquipmentReservation> getAll() {
        return equipmentReservationService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get equipment reservation by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipment reservation found"),
        @ApiResponse(responseCode = "404", description = "Equipment reservation not found")
    })
    public ResponseEntity<EquipmentReservation> getById(@PathVariable Long id) {
        return equipmentReservationService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create equipment reservation")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Equipment reservation created")})
    public EquipmentReservation create(@Valid @RequestBody EquipmentReservationRequest request) {
        return equipmentReservationService.create(toEntity(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update equipment reservation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipment reservation updated"),
        @ApiResponse(responseCode = "404", description = "Equipment reservation not found")
    })
    public ResponseEntity<EquipmentReservation> update(@PathVariable Long id, @Valid @RequestBody EquipmentReservationRequest request) {
        if (equipmentReservationService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(equipmentReservationService.update(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete equipment reservation")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Equipment reservation deleted"),
        @ApiResponse(responseCode = "404", description = "Equipment reservation not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (equipmentReservationService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        equipmentReservationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EquipmentReservation toEntity(EquipmentReservationRequest request) {
        EquipmentReservation equipmentReservation = new EquipmentReservation();
        equipmentReservation.setQuantity(request.getQuantity());

        Reservation reservation = new Reservation();
        reservation.setId(request.getReservationId());
        equipmentReservation.setReservation(reservation);

        Equipment equipment = new Equipment();
        equipment.setId(request.getEquipmentId());
        equipmentReservation.setEquipment(equipment);

        Stall stall = new Stall();
        stall.setId(request.getStallId());
        equipmentReservation.setStall(stall);

        return equipmentReservation;
    }
}
