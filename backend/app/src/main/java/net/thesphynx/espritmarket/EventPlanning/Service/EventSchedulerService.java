package net.thesphynx.espritmarket.EventPlanning.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import net.thesphynx.espritmarket.EventPlanning.Entity.Equipment;
import net.thesphynx.espritmarket.EventPlanning.Entity.Stall;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEventRepository;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEquipmentRepository;
import net.thesphynx.espritmarket.EventPlanning.Repository.IStallRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSchedulerService {
    private final IEventRepository eventRepository;
    private final IEquipmentRepository equipmentRepository;
    private final IStallRepository stallRepository;
    private final EventNotificationService eventNotificationService;

    /**
     * Scheduled task that runs every day at midnight (00:00:00)
     * Marks events as FINISHED if:
     * - Event is in-person (not online)
     * - Event date is before today
     * - Event status is "ONGOING"
     * Then updates all related equipment and stalls to "AVAILABLE"
     */
    @Scheduled(cron = "0 0 0 * * *")  // Every day at 00:00:00
    @Transactional
    public void finishExpiredEvents() {
        log.info("========== [SCHEDULER] Running finishExpiredEvents at midnight ==========");
        
        try {
            LocalDate today = LocalDate.now();
            
            // Find all in-person events that should be finished
            List<Event> allEvents = eventRepository.findAll();
            List<Event> expiredEvents = allEvents.stream()
                    .filter(event -> !event.isOnline())  // Only in-person events
                    .filter(event -> event.getDate().isBefore(today))  // Date is in the past
                    .filter(event -> "ONGOING".equals(event.getStatus()))  // Status is ONGOING
                    .toList();
            
            log.info("Found {} expired events to finish", expiredEvents.size());
            
            for (Event event : expiredEvents) {
                // Mark event as FINISHED
                event.setStatus("FINISHED");
                eventRepository.save(event);
                log.info("✓ Event '{}' (ID: {}) marked as FINISHED", event.getName(), event.getId());
                
                // Update all related equipment to AVAILABLE
                List<Equipment> equipments = equipmentRepository.findAll().stream()
                        .filter(eq -> eq.getEvent() != null && eq.getEvent().getId().equals(event.getId()))
                        .toList();
                
                for (Equipment equipment : equipments) {
                    equipment.setStatus("AVAILABLE");
                    equipmentRepository.save(equipment);
                }
                log.info("✓ Updated {} equipment to AVAILABLE for event {}", equipments.size(), event.getId());
                
                // Update all related stalls to AVAILABLE
                List<Stall> stalls = stallRepository.findAll().stream()
                        .filter(stall -> stall.getEvent() != null && stall.getEvent().getId().equals(event.getId()))
                        .toList();
                
                for (Stall stall : stalls) {
                    stall.setStatus("AVAILABLE");
                    stallRepository.save(stall);
                }
                log.info("✓ Updated {} stalls to AVAILABLE for event {}", stalls.size(), event.getId());
            }
            
            log.info("========== [SCHEDULER] finishExpiredEvents completed successfully ==========");
        } catch (Exception e) {
            log.error("========== [SCHEDULER] ERROR in finishExpiredEvents ==========", e);
        }
    }

    /**
     * Manual trigger method - For testing or immediate execution
     */
    @Transactional
    public void triggerNow() {
        log.info("========== [MANUAL TRIGGER] Force-running finishExpiredEvents ==========");
        finishExpiredEvents();
    }

    /**
     * Scheduled task that runs every day at 08:00:00
     * Sends alert notifications to event creators for events happening today or tomorrow
     * Works for all event types (online and in-person)
     */
    @Scheduled(cron = "0 0 8 * * *")  // Every day at 08:00:00
    @Transactional
    public void notifyUpcomingEvents() {
        log.info("========== [SCHEDULER] Running notifyUpcomingEvents at 08:00 ==========");
        
        try {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            
            // Find all events happening today or tomorrow
            List<Event> allEvents = eventRepository.findAll();
            List<Event> upcomingEvents = allEvents.stream()
                    .filter(event -> event.getDate() != null 
                            && (event.getDate().equals(today) || event.getDate().equals(tomorrow)))
                    .filter(event -> "UPCOMING".equals(event.getStatus()))  // Only UPCOMING events
                    .toList();
            
            log.info("Found {} events happening today or tomorrow", upcomingEvents.size());
            
            int notificationsSent = 0;
            for (Event event : upcomingEvents) {
                if (eventNotificationService.notifyCreatorForUpcomingEvent(event)) {
                    notificationsSent++;
                }
            }
            
            log.info("✓ Sent {} alert notifications", notificationsSent);
            log.info("========== [SCHEDULER] notifyUpcomingEvents completed successfully ==========");
        } catch (Exception e) {
            log.error("========== [SCHEDULER] ERROR in notifyUpcomingEvents ==========", e);
        }
    }
}

