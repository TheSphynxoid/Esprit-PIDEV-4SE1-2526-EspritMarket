package net.thesphynx.espritmarket.EventPlanning.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "date is required")
    @FutureOrPresent(message = "date must be today or later")
    private LocalDate date;

    @NotNull(message = "eventId is required")
    @JsonProperty("eventId")
    private Long eventId;
    
    // Support for frontend format: event: { id: 123 }
    @Getter
    @Setter
    @NoArgsConstructor
    public static class EventRef {
        private Long id;
    }
    
    // Custom setter to handle both eventId and event.id formats
    @JsonProperty("event")
    public void setEvent(EventRef event) {
        if (event != null && event.getId() != null) {
            this.eventId = event.getId();
        }
    }
}