package net.thesphynx.espritmarket.EventPlanning.Controller;

import net.thesphynx.espritmarket.EventPlanning.Dto.CollaborationRequest;
import net.thesphynx.espritmarket.EventPlanning.Entity.Collaboration;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import net.thesphynx.espritmarket.EventPlanning.Service.CollaborationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventplanning/collaborations")
@Tag(name = "EventPlanning - Collaborations")
public class CollaborationController {
    private final CollaborationService collaborationService;

    public CollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @GetMapping
    @Operation(summary = "List collaborations")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Collaborations retrieved")})
    public List<Collaboration> getAll() {
        return collaborationService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get collaboration by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Collaboration found"),
        @ApiResponse(responseCode = "404", description = "Collaboration not found")
    })
    public ResponseEntity<Collaboration> getById(@PathVariable Long id) {
        return collaborationService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create collaboration")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Collaboration created")})
    public Collaboration create(@Valid @RequestBody CollaborationRequest request) {
        return collaborationService.create(toEntity(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update collaboration")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Collaboration updated"),
        @ApiResponse(responseCode = "404", description = "Collaboration not found")
    })
    public ResponseEntity<Collaboration> update(@PathVariable Long id, @Valid @RequestBody CollaborationRequest request) {
        if (collaborationService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(collaborationService.update(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete collaboration")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Collaboration deleted"),
        @ApiResponse(responseCode = "404", description = "Collaboration not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (collaborationService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        collaborationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Collaboration toEntity(CollaborationRequest request) {
        Collaboration collaboration = new Collaboration();
        collaboration.setName(request.getName());
        collaboration.setType(request.getType());
        collaboration.setDescription(request.getDescription());

        Event event = new Event();
        event.setId(request.getEventId());
        collaboration.setEvent(event);

        return collaboration;
    }
}
