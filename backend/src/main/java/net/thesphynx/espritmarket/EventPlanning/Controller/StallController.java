package net.thesphynx.espritmarket.EventPlanning.Controller;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.EventPlanning.Dto.StallRequest;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import net.thesphynx.espritmarket.EventPlanning.Entity.Stall;
import net.thesphynx.espritmarket.EventPlanning.Service.StallService;
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
@RequestMapping("/api/eventplanning/stalls")
@Tag(name = "EventPlanning - Stalls")
public class StallController {
    private final StallService stallService;
    private final EntityManager entityManager;

    public StallController(StallService stallService, EntityManager entityManager) {
        this.stallService = stallService;
        this.entityManager = entityManager;
    }

    @GetMapping
    @Operation(summary = "List stalls")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Stalls retrieved")})
    public List<Stall> getAll() {
        return stallService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stall by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stall found"),
        @ApiResponse(responseCode = "404", description = "Stall not found")
    })
    public ResponseEntity<Stall> getById(@PathVariable Long id) {
        return stallService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create stall")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Stall created")})
    public Stall create(@Valid @RequestBody StallRequest request) {
        return stallService.create(toEntity(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update stall")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stall updated"),
        @ApiResponse(responseCode = "404", description = "Stall not found")
    })
    public ResponseEntity<Stall> update(@PathVariable Long id, @Valid @RequestBody StallRequest request) {
        if (stallService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stallService.update(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete stall")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Stall deleted"),
        @ApiResponse(responseCode = "404", description = "Stall not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (stallService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        stallService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Stall toEntity(StallRequest request) {
        Stall stall = new Stall();
        stall.setName(request.getName());
        stall.setNumber(request.getNumber());
        stall.setLocation(request.getLocation());

        // Use EntityManager.getReference() to create a Hibernate proxy instead of a detached entity
        Event event = entityManager.getReference(Event.class, request.getEventId());
        stall.setEvent(event);

        if (request.getUserId() != null) {
            User user = entityManager.getReference(User.class, request.getUserId());
            stall.setUser(user);
        }

        return stall;
    }
}
