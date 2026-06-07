package net.thesphynx.espritmarket.Srv.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDto {
    private LocalDateTime start;
    private LocalDateTime end;
    private int maxConcurrent;
    private int currentBookings;
    private int availableCapacity;
    private boolean available;
    private int slotDurationMinutes;
}
