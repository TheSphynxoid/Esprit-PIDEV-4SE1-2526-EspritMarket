package net.thesphynx.espritmarket.EventPlanning.Service.Interface;

import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import java.util.List;
import java.util.Optional;

public interface IEventService {
    Event addEvent(Event event);
    Event updateEvent(Event event);
    void deleteEvent(Long id);
    Optional<Event> getEventById(Long id);
    List<Event> getAllEvents();
    
    // Additional methods used by controller
    Event create(Event event);
    Event update(Long id, Event event);
    void delete(Long id);
    Optional<Event> getById(Long id);
    List<Event> getAll();
}