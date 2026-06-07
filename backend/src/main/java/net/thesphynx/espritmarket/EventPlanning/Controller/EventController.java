package net.thesphynx.espritmarket.EventPlanning.Controller;

import net.thesphynx.espritmarket.EventPlanning.Dto.EventRequest;
import net.thesphynx.espritmarket.EventPlanning.Dto.EventStatusResponse;
import net.thesphynx.espritmarket.EventPlanning.Dto.TicketReminderResponse;
import net.thesphynx.espritmarket.EventPlanning.Dto.UpcomingEventAlertDto;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import net.thesphynx.espritmarket.EventPlanning.Entity.Equipment;
import net.thesphynx.espritmarket.EventPlanning.Entity.Stall;
import net.thesphynx.espritmarket.EventPlanning.Service.EventService;
import net.thesphynx.espritmarket.EventPlanning.Service.TicketReminderService;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEquipmentRepository;
import net.thesphynx.espritmarket.EventPlanning.Repository.IStallRepository;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/eventplanning/events")
@Tag(name = "EventPlanning - Events")
@PreAuthorize("isAuthenticated()")
public class EventController {
    private final EventService eventService;
    private final TicketReminderService ticketReminderService;
    private final IEquipmentRepository equipmentRepository;
    private final IStallRepository stallRepository;
    private final UserRepository userRepository;

    public EventController(EventService eventService, TicketReminderService ticketReminderService, IEquipmentRepository equipmentRepository, IStallRepository stallRepository, UserRepository userRepository) {
        this.eventService = eventService;
        this.ticketReminderService = ticketReminderService;
        this.equipmentRepository = equipmentRepository;
        this.stallRepository = stallRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "List events")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Events retrieved")})
    public List<Event> getAll() {
        return eventService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event found"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Event> getById(@PathVariable Long id) {
        return eventService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create event")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Event created")})
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'EVENT')")
    public Event create(@Valid @RequestBody EventRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        Event entity = toEntity(request);
        entity.setCreator(currentUser);

        org.slf4j.LoggerFactory.getLogger(this.getClass()).info(
            "EventController.create() received: name='{}', online={}, date={}, creator={}",
            entity.getName(), entity.isOnline(), entity.getDate(), currentUser.getEmail());
        return eventService.create(entity);
    }

    @PostMapping("/{id}/notify-online")
    @Operation(summary = "Send online event notification emails")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification sent"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<String> notifyOnlineEvent(@PathVariable Long id) {
        int sentCount = eventService.notifyOnlineEventById(id);
        if (sentCount < 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("sent to " + sentCount + " recipients");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update event")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event updated"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'EVENT')")
    public ResponseEntity<Event> update(@PathVariable Long id, @Valid @RequestBody EventRequest request, Authentication authentication) {
        if (eventService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User currentUser = getCurrentUser(authentication);
        Event existingEvent = eventService.getById(id).get();
        
        // Check authorization: only creator can update
        if (!existingEvent.getCreator().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).build(); // Forbidden
        }
        
        Event updatedEntity = toEntity(request);
        updatedEntity.setCreator(currentUser);
        return ResponseEntity.ok(eventService.update(id, updatedEntity));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete event")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Event deleted"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'EVENT')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        var event = eventService.getById(id);
        if (event.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User currentUser = getCurrentUser(authentication);
        
        // Check authorization: only creator can delete
        if (!event.get().getCreator().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).build(); // Forbidden
        }
        
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-events")
    @Operation(summary = "Get all events created by the current user")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "User events retrieved")})
    public List<Event> getMyEvents(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        org.slf4j.LoggerFactory.getLogger(this.getClass()).info("Fetching events for user: {}", currentUser.getEmail());
        return eventService.getEventsByUserWithEquipments(currentUser.getId());
    }

    @GetMapping("/my-events/equipments")
    @Operation(summary = "Get all equipments for events created by the current user")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "User equipments retrieved")})
    public List<Equipment> getMyEquipments(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        org.slf4j.LoggerFactory.getLogger(this.getClass()).info("Fetching equipments for user: {}", currentUser.getEmail());
        return eventService.getEquipmentsByUser(currentUser.getId());
    }

    @GetMapping("/with-participants")
    @Operation(summary = "Get all events with tickets and participants")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Events with participants retrieved")})
    public List<Event> getEventsWithParticipants() {
        return eventService.getEventsWithTicketsAndUsers();
    }

    @GetMapping("/{id}/with-participants")
    @Operation(summary = "Get event with tickets and participants")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event with participants found"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Event> getEventWithParticipants(@PathVariable Long id) {
        return eventService.getEventWithTicketsAndUsersById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/status/dashboard")
    @Transactional(readOnly = true)
    @Operation(summary = "Get all events with their status, equipment and stalls")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Events status retrieved")})
    public List<EventStatusResponse> getEventsStatusDashboard() {
        // Load all data
        List<Event> events = eventService.getAll();
        List<Equipment> allEquipments = equipmentRepository.findAll();
        List<Stall> allStalls = stallRepository.findAll();

        // Group equipment by event ID
        Map<Long, List<Equipment>> equipmentsByEventId = allEquipments.stream()
                .filter(eq -> eq.getEvent() != null)
                .collect(Collectors.groupingBy(eq -> eq.getEvent().getId()));

        // Group stalls by event ID
        Map<Long, List<Stall>> stallsByEventId = allStalls.stream()
                .filter(stall -> stall.getEvent() != null)
                .collect(Collectors.groupingBy(stall -> stall.getEvent().getId()));

        // Build response DTOs
        return events.stream()
                .map(event -> EventStatusResponse.builder()
                        .id(event.getId())
                        .name(event.getName())
                        .date(event.getDate())
                        .location(event.getLocation())
                        .online(event.isOnline())
                        .status(event.getStatus())
                        .equipments(equipmentsByEventId.getOrDefault(event.getId(), List.of()).stream()
                                .map(eq -> EventStatusResponse.EquipmentStatusDto.builder()
                                        .id(eq.getId())
                                        .name(eq.getName())
                                        .type(eq.getType())
                                        .status(eq.getStatus())
                                        .build())
                                .collect(Collectors.toList()))
                        .stalls(stallsByEventId.getOrDefault(event.getId(), List.of()).stream()
                                .map(stall -> EventStatusResponse.StallStatusDto.builder()
                                        .id(stall.getId())
                                        .name(stall.getName())
                                        .number(stall.getNumber())
                                        .location(stall.getLocation())
                                        .status(stall.getStatus())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    @GetMapping("/ticket-reminders")
    @Transactional(readOnly = true)
    @Operation(summary = "Get ticket sale reminders")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Ticket reminders retrieved")})
    public List<TicketReminderResponse> getTicketSaleReminders() {
        return ticketReminderService.getTicketSaleReminders();
    }

    @GetMapping("/upcoming-alerts")
    @Transactional(readOnly = true)
    @Operation(summary = "Get upcoming event alerts for authenticated user (today and tomorrow)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Upcoming event alerts retrieved")})
    public List<UpcomingEventAlertDto> getUpcomingEventAlerts(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        
        return eventService.getAll().stream()
                .filter(event -> event.getCreator().getId().equals(currentUser.getId()))  // Only user's events
                .filter(event -> event.getDate() != null && 
                        (event.getDate().equals(today) || event.getDate().equals(tomorrow)))  // Today or tomorrow
                .filter(event -> "UPCOMING".equals(event.getStatus()))  // Only UPCOMING events
                .map(event -> {
                    String daysText = event.getDate().equals(today) ? "aujourd'hui" : "demain";
                    return UpcomingEventAlertDto.fromEvent(event, daysText);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get the current authenticated user from the Authentication object
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        final String userEmail;
        if (authentication.getPrincipal() instanceof UserDetails) {
            userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            userEmail = authentication.getName();
        }
        
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
    }

    private Event toEntity(EventRequest request) {
        Event event = new Event();
        event.setName(request.getName());
        event.setDate(request.getDate());
        event.setLocation(request.getLocation());
        event.setOnline(request.isOnline());
        event.setStatus("UPCOMING"); // Initialize with default status
        event.setNbTickets(request.getNbTickets());
        return event;
    }
}
