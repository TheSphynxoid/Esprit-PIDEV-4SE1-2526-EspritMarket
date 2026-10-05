package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Srv.Dto.BookingMessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookingRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public BookingRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishMessage(BookingMessageResponse message) {
        if (message == null || message.getBookingId() == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/srv/bookings/" + message.getBookingId() + "/messages", message);
    }
}
