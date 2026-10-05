package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;

@Data
public class BookingStatusUpdateRequest {
    private BookingStatus status;
}
