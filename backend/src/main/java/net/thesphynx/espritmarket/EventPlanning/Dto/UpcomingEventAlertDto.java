package net.thesphynx.espritmarket.EventPlanning.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpcomingEventAlertDto {
    
    private Long id;
    private String name;
    private LocalDate date;
    private String location;
    private boolean online;
    private int nbTickets;
    private String daysText;  // "aujourd'hui" or "demain"
    
    public static UpcomingEventAlertDto fromEvent(Event event, String daysText) {
        return UpcomingEventAlertDto.builder()
                .id(event.getId())
                .name(event.getName())
                .date(event.getDate())
                .location(event.getLocation())
                .online(event.isOnline())
                .nbTickets(event.getNbTickets())
                .daysText(daysText)
                .build();
    }
}
