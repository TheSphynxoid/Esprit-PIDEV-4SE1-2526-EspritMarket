package net.thesphynx.espritmarket.EventPlanning.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Dto.TicketReminderResponse;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketReminderService {
    private final IEventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<TicketReminderResponse> getTicketSaleReminders() {
        List<TicketReminderResponse> reminders = new ArrayList<>();

        for (Event event : eventRepository.findEventsWithTicketsAndUsers()) {
            int remainingTickets = Math.max(0, event.getNbTickets() - (event.getTickets() != null ? event.getTickets().size() : 0));

            if (remainingTickets == 0) {
                reminders.add(build(event, remainingTickets, "Tickets bientôt épuisés", "Tickets bientôt épuisés", "danger", "SOLD_OUT"));
                reminders.add(build(event, remainingTickets, "Vente terminée", "Vente terminée", "danger", "SOLD_OUT"));
            } else if (remainingTickets <= 3) {
                reminders.add(build(event, remainingTickets, "Tickets bientôt épuisés", "Tickets bientôt épuisés", "warning", "LOW_STOCK"));
            } else if (remainingTickets <= 10) {
                reminders.add(build(event, remainingTickets, "Il reste 10 tickets", "Il reste 10 tickets", "info", "ALMOST_FULL"));
            }
        }

        reminders.sort(Comparator.comparing(TicketReminderResponse::getRemainingTickets));
        log.info("Generated {} ticket sale reminders", reminders.size());
        return reminders;
    }

    private TicketReminderResponse build(Event event, int remainingTickets, String title, String message, String severity, String status) {
        return TicketReminderResponse.builder()
                .eventId(event.getId())
                .eventName(event.getName())
                .remainingTickets(remainingTickets)
                .title(title)
                .message(message)
                .severity(severity)
                .status(status)
                .build();
    }
}
