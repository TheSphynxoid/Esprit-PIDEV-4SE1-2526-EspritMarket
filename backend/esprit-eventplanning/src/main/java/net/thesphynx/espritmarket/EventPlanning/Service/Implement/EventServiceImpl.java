package net.thesphynx.espritmarket.EventPlanning.Service.Implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEventRepository;
import net.thesphynx.espritmarket.EventPlanning.Service.Interface.IEventService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements IEventService {

    private final IEventRepository eventRepository;

    @Override
    public Event addEvent(Event event) {
        log.info("Adding Event: {}", event);
        return eventRepository.save(event);
    }

    @Override
    public Event updateEvent(Event event) {
        log.info("Updating Event: {}", event);
        return eventRepository.save(event);
    }

    @Override
    public void deleteEvent(Long id) {
        log.info("Deleting Event with id: {}", id);
        eventRepository.deleteById(id);
    }

    @Override
    public Optional<Event> getEventById(Long id) {
        log.info("Getting Event with id: {}", id);
        return eventRepository.findById(id);
    }

    @Override
    public List<Event> getAllEvents() {
        log.info("Getting all Events");
        return eventRepository.findAll();
    }

    // Additional methods used by controller
    @Override
    public Event create(Event event) {
        log.info("Creating new Event: {}", event.getName());
        return eventRepository.save(event);
    }

    @Override
    public Event update(Long id, Event event) {
        log.info("Updating Event with id: {}", id);
        event.setId(id);
        return eventRepository.save(event);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting Event with id: {}", id);
        eventRepository.deleteById(id);
    }

    @Override
    public Optional<Event> getById(Long id) {
        log.info("Getting Event with id: {}", id);
        return eventRepository.findById(id);
    }

    @Override
    public List<Event> getAll() {
        log.info("Getting all Events");
        return eventRepository.findAll();
    }
}