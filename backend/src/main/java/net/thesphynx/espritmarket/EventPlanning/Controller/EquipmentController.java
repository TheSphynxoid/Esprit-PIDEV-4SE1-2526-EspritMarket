package net.thesphynx.espritmarket.EventPlanning.Controller;

import net.thesphynx.espritmarket.EventPlanning.Dto.EquipmentRequest;
import net.thesphynx.espritmarket.EventPlanning.Entity.Equipment;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import net.thesphynx.espritmarket.EventPlanning.Service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventplanning/equipment")
@Tag(name = "EventPlanning - Equipment")
public class EquipmentController {
    private final EquipmentService equipmentService;
    private final EntityManager entityManager;

    public EquipmentController(EquipmentService equipmentService, EntityManager entityManager) {
        this.equipmentService = equipmentService;
        this.entityManager = entityManager;
    }

    @GetMapping
    @Operation(summary = "List equipment")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Equipment retrieved")})
    public List<Equipment> getAll() {
        return equipmentService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get equipment by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipment found"),
        @ApiResponse(responseCode = "404", description = "Equipment not found")
    })
    public ResponseEntity<Equipment> getById(@PathVariable Long id) {
        return equipmentService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create equipment")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Equipment created")})
    public Equipment create(@Valid @RequestBody EquipmentRequest request) {
        return equipmentService.create(toEntity(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update equipment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipment updated"),
        @ApiResponse(responseCode = "404", description = "Equipment not found")
    })
    public ResponseEntity<Equipment> update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        if (equipmentService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(equipmentService.update(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete equipment")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Equipment deleted"),
        @ApiResponse(responseCode = "404", description = "Equipment not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (equipmentService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Equipment toEntity(EquipmentRequest request) {
        Equipment equipment = new Equipment();
        equipment.setName(request.getName());
        equipment.setType(request.getType());
        equipment.setStatus(request.getStatus());
        equipment.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);

        // Use EntityManager.getReference() to create a Hibernate proxy instead of a detached entity
        if (request.getEventId() != null) {
            Event event = entityManager.getReference(Event.class, request.getEventId());
            equipment.setEvent(event);
        }

        return equipment;
    }
}
