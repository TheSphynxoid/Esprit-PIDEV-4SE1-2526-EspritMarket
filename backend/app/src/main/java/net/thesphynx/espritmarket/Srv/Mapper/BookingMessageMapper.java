package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.BookingMessageResponse;
import net.thesphynx.espritmarket.Srv.Entity.BookingMessage;
import org.springframework.stereotype.Component;

@Component
public class BookingMessageMapper {
    public BookingMessageResponse toResponse(BookingMessage entity) {
        if (entity == null) return null;

        BookingMessageResponse response = new BookingMessageResponse();
        response.setId(entity.getId());
        if (entity.getBooking() != null) {
            response.setBookingId(entity.getBooking().getId());
        }
        if (entity.getSender() != null) {
            response.setSenderId(entity.getSender().getId());
            response.setSenderName(entity.getSender().getName());
        }
        response.setMessage(entity.getMessage());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
