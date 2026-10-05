package net.thesphynx.espritmarket.EventPlanning.Service;

import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import net.thesphynx.espritmarket.EventPlanning.Entity.Equipment;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEventRepository;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEquipmentRepository;
import net.thesphynx.espritmarket.Common.Entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EventService {
    private final IEventRepository eventRepository;
    private final IEquipmentRepository equipmentRepository;
    private final EventNotificationService eventNotificationService;

    public EventService(IEventRepository eventRepository, IEquipmentRepository equipmentRepository, EventNotificationService eventNotificationService) {
        this.eventRepository = eventRepository;
        this.equipmentRepository = equipmentRepository;
        this.eventNotificationService = eventNotificationService;
    }

    public List<Event> getAll() {
        return eventRepository.findAll();
    }

    public Optional<Event> getById(Long id) {
        return eventRepository.findById(id);
    }

    public Event create(Event event) {
        log.info("EventService.create() called with event: name='{}', online={}, date={}", 
                event.getName(), event.isOnline(), event.getDate());
        
        // Calculate number of tickets: use actual tickets if available, otherwise use nbTickets
        int nbTickets = 0;
        if (event.getTickets() != null && !event.getTickets().isEmpty()) {
            nbTickets = event.getTickets().size();
            log.info("Event has {} actual tickets", nbTickets);
        } else {
            nbTickets = event.getNbTickets();
            log.info("Using estimated nbTickets: {}", nbTickets);
        }
        
        // Auto-recommend equipment for in-person events with tickets
        if (!event.isOnline() && nbTickets > 0) {
            log.info("Generating recommended equipment for in-person event with {} tickets", nbTickets);
            List<Equipment> recommendedEquipments = recommendEquipments(nbTickets);
            
            for (Equipment equipment : recommendedEquipments) {
                equipment.setEvent(event);
                event.getEquipments().add(equipment);
                log.info("Adding recommended equipment: name={}, quantity={}, type={}", 
                        equipment.getName(), equipment.getQuantity(), equipment.getType());
            }
            log.info("Added {} recommended equipment items to event", recommendedEquipments.size());
        } else {
            log.info("Skipping equipment recommendation: online={}, nbTickets={}", event.isOnline(), nbTickets);
        }
        
        // Save event and all associated equipment
        Event createdEvent = eventRepository.save(event);
        log.info("Event saved with id={}, equipment count={}, nbTickets={}", 
                createdEvent.getId(), createdEvent.getEquipments().size(), createdEvent.getNbTickets());
        
        // Verify all equipment was saved correctly
        for (Equipment eq : createdEvent.getEquipments()) {
            log.info("Verified equipment in DB: id={}, name={}, quantity={}, type={}, status={}", 
                    eq.getId(), eq.getName(), eq.getQuantity(), eq.getType(), eq.getStatus());
        }
        
        int notified = eventNotificationService.notifyUsersForOnlineEvent(createdEvent);
        log.info("Notification returned: {} recipients notified", notified);
        return createdEvent;
    }

    public int notifyOnlineEventById(Long eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            return -1;
        }
        return eventNotificationService.notifyUsersForOnlineEvent(eventOptional.get());
    }

    public Event update(Long id, Event event) {
        event.setId(id);
        return eventRepository.save(event);
    }

    public void delete(Long id) {
        eventRepository.deleteById(id);
    }

    /**
     * Get all events with their tickets and associated users (participants)
     * Supports viewing event participation details
     */
    public List<Event> getEventsWithTicketsAndUsers() {
        return eventRepository.findEventsWithTicketsAndUsers();
    }

    /**
     * Get a specific event with all its tickets and associated users
     * Useful for event detail views with participant list
     */
    public Optional<Event> getEventWithTicketsAndUsersById(Long id) {
        return eventRepository.findEventWithTicketsAndUsersById(id);
    }

    /**
     * Get all events created by a specific user
     */
    public List<Event> getEventsByUser(Long userId) {
        log.info("Fetching all events for user: {}", userId);
        return eventRepository.findEventsByCreatorId(userId);
    }

    /**
     * Get all events created by a user with their equipments
     */
    public List<Event> getEventsByUserWithEquipments(Long userId) {
        log.info("Fetching all events with equipments for user: {}", userId);
        return eventRepository.findEventsByCreatorIdWithEquipments(userId);
    }

    /**
     * Get all equipments for events created by a user
     */
    public List<Equipment> getEquipmentsByUser(Long userId) {
        log.info("Fetching all equipments for user: {}", userId);
        return equipmentRepository.findEquipmentsByCreatorId(userId);
    }

    /**
     * Recommends equipment quantities based on the number of tickets
     * Applied only for in-person events (not online)
     * 
     * Rules:
     * - Chairs = nbTickets * 1.1 (rounded up)
     * - Tables = nbTickets / 5 (integer division)
     * - Microphones = max(nbTickets / 20, 1) (minimum 1)
     * - Projectors = nbTickets <= 30 ? 1 : 2
     * 
     * @param nbTickets number of event tickets
     * @return list of recommended equipment
     */
    public List<Equipment> recommendEquipments(int nbTickets) {
        List<Equipment> recommendedEquipments = new ArrayList<>();

        // Chairs: nbTickets * 1.1, rounded up
        int chairsQuantity = (int) Math.ceil(nbTickets * 1.1);
        Equipment chairs = new Equipment();
        chairs.setName("Chairs");
        chairs.setType("Furniture");
        chairs.setStatus("AVAILABLE");
        chairs.setQuantity(chairsQuantity);
        log.info("Recommending Chairs with quantity: {}", chairsQuantity);
        recommendedEquipments.add(chairs);

        // Tables: nbTickets / 5
        if (nbTickets > 0) {
            int tablesQuantity = nbTickets / 5;
            Equipment tables = new Equipment();
            tables.setName("Tables");
            tables.setType("Furniture");
            tables.setStatus("AVAILABLE");
            tables.setQuantity(Math.max(tablesQuantity, 1)); // At least 1 table
            log.info("Recommending Tables with quantity: {}", Math.max(tablesQuantity, 1));
            recommendedEquipments.add(tables);
        }

        // Microphones: max(nbTickets / 20, 1)
        int microphonesQuantity = Math.max(nbTickets / 20, 1);
        Equipment microphones = new Equipment();
        microphones.setName("Microphone");
        microphones.setType("Audio");
        microphones.setStatus("AVAILABLE");
        microphones.setQuantity(microphonesQuantity);
        log.info("Recommending Microphone with quantity: {}", microphonesQuantity);
        recommendedEquipments.add(microphones);

        // Projectors: 1 if nbTickets <= 30, else 2
        int projectorsQuantity = nbTickets <= 30 ? 1 : 2;
        Equipment projectors = new Equipment();
        projectors.setName("Projector");
        projectors.setType("Visual");
        projectors.setStatus("AVAILABLE");
        projectors.setQuantity(projectorsQuantity);
        log.info("Recommending Projector with quantity: {}", projectorsQuantity);
        recommendedEquipments.add(projectors);

        log.info("Recommended {} equipment items for {} tickets", recommendedEquipments.size(), nbTickets);
        return recommendedEquipments;
    }
}
