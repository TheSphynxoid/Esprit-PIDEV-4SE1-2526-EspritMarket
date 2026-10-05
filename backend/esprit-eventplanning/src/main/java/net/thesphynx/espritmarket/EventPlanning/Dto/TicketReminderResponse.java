package net.thesphynx.espritmarket.EventPlanning.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketReminderResponse {
    private Long eventId;
    private String eventName;
    private int remainingTickets;
    private String title;
    private String message;
    private String severity;
    private String status;
}
