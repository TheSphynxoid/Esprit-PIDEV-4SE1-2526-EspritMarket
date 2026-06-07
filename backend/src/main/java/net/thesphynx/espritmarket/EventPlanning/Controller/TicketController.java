package net.thesphynx.espritmarket.EventPlanning.Controller;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.EventPlanning.Dto.TicketPromoSelectionRequest;
import net.thesphynx.espritmarket.EventPlanning.Dto.TicketRequest;
import net.thesphynx.espritmarket.EventPlanning.Dto.TicketPromoOfferResponse;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import net.thesphynx.espritmarket.EventPlanning.Entity.Ticket;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEventRepository;
import net.thesphynx.espritmarket.EventPlanning.Service.TunisiaTicketDiscountService;
import net.thesphynx.espritmarket.EventPlanning.Service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventplanning/tickets")
@Tag(name = "EventPlanning - Tickets")
public class TicketController {
    private final TicketService ticketService;
    private final IEventRepository eventRepository;
    private final UserRepository userRepository;
    private final TunisiaTicketDiscountService tunisiaTicketDiscountService;

    public TicketController(TicketService ticketService, IEventRepository eventRepository, UserRepository userRepository, TunisiaTicketDiscountService tunisiaTicketDiscountService) {
        this.ticketService = ticketService;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.tunisiaTicketDiscountService = tunisiaTicketDiscountService;
    }

    @GetMapping
    @Operation(summary = "List tickets")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Tickets retrieved")})
    public List<Ticket> getAll() {
        return ticketService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket found"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<Ticket> getById(@PathVariable Long id) {
        return ticketService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create ticket")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Ticket created")})
    public Ticket create(@Valid @RequestBody TicketRequest request) {
        return ticketService.create(toEntity(request));
    }

    @GetMapping("/promo-dates")
    @Operation(summary = "List Tunisian holiday dates used for ticket promos")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Promo dates retrieved")})
    public List<String> getPromoDates() {
        return tunisiaTicketDiscountService.getHolidayDates();
    }

    @GetMapping("/promo-offers")
    @Operation(summary = "List Tunisian holiday promo offers")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Promo offers retrieved")})
    public List<TicketPromoOfferResponse> getPromoOffers() {
        return tunisiaTicketDiscountService.getPromoOffers();
    }

    @PostMapping("/promo-selection")
    @Operation(summary = "Receive the selected ticket promo percentage")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Promo selection received")})
    public ResponseEntity<Void> submitPromoSelection(@Valid @RequestBody TicketPromoSelectionRequest request) {
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ticket")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket updated"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<Ticket> update(@PathVariable Long id, @Valid @RequestBody TicketRequest request) {
        if (ticketService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ticketService.update(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ticket")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Ticket deleted"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (ticketService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Ticket toEntity(TicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setType(request.getType());
        ticket.setPrice(request.getPrice());
        ticket.setOriginalPrice(request.getOriginalPrice() != null ? request.getOriginalPrice() : request.getPrice());
        ticket.setFinalPrice(request.getPrice());
        ticket.setDiscountApplied(Boolean.TRUE.equals(request.getDiscountApplied()) && request.getDiscountRate() != null && request.getDiscountRate() > 0);
        ticket.setDiscountRate(request.getDiscountRate() != null ? request.getDiscountRate() : 0.0);
        ticket.setDiscountLabel(request.getDiscountLabel());

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + request.getEventId()));
        ticket.setEvent(event);

        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));
            ticket.setUser(user);
        }

        return ticket;
    }
}
