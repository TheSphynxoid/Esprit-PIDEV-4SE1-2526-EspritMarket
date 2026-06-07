package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Srv.Dto.BookingMessageBatchResponse;
import net.thesphynx.espritmarket.Srv.Dto.BookingMessageRequest;
import net.thesphynx.espritmarket.Srv.Dto.BookingMessageResponse;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.BookingMessage;
import net.thesphynx.espritmarket.Srv.Mapper.BookingMessageMapper;
import net.thesphynx.espritmarket.Srv.Repository.IBookingMessageRepository;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BookingMessageService {
    private final IBookingMessageRepository messageRepository;
    private final IBookingRepository bookingRepository;
    private final BookingMessageMapper mapper;
    private final BookingRealtimeService bookingRealtimeService;

    public BookingMessageService(IBookingMessageRepository messageRepository,
                                 IBookingRepository bookingRepository,
                                 BookingMessageMapper mapper,
                                 BookingRealtimeService bookingRealtimeService) {
        this.messageRepository = messageRepository;
        this.bookingRepository = bookingRepository;
        this.mapper = mapper;
        this.bookingRealtimeService = bookingRealtimeService;
    }

    public List<BookingMessageResponse> getByBookingId(Long bookingId, Long userId) {
        Booking booking = findBooking(bookingId);
        validateParticipant(booking, userId);

        return messageRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public BookingMessageBatchResponse getBatch(Long bookingId, Long userId, Long beforeId, int limit) {
        Booking booking = findBooking(bookingId);
        validateParticipant(booking, userId);

        int safeLimit = Math.max(1, Math.min(limit, 100));
        PageRequest pageRequest = PageRequest.of(0, safeLimit + 1);

        List<BookingMessage> raw = beforeId == null
                ? messageRepository.findLatestBatch(bookingId, pageRequest)
                : messageRepository.findBatchBeforeId(bookingId, beforeId, pageRequest);

        boolean hasMore = raw.size() > safeLimit;
        List<BookingMessage> batch = new ArrayList<>(hasMore ? raw.subList(0, safeLimit) : raw);

        Collections.reverse(batch);
        List<BookingMessageResponse> items = batch.stream()
                .map(mapper::toResponse)
                .toList();

        Long nextCursor = hasMore && !batch.isEmpty() ? batch.get(0).getId() : null;

        BookingMessageBatchResponse response = new BookingMessageBatchResponse();
        response.setItems(items);
        response.setNextCursor(nextCursor);
        response.setHasMore(hasMore);
        return response;
    }

    public List<BookingMessageResponse> getNewer(Long bookingId, Long userId, Long afterId, int limit) {
        if (afterId == null) return Collections.emptyList();
        Booking booking = findBooking(bookingId);
        validateParticipant(booking, userId);

        int safeLimit = Math.max(1, Math.min(limit, 100));
        PageRequest pageRequest = PageRequest.of(0, safeLimit);

        return messageRepository.findBatchAfterId(bookingId, afterId, pageRequest).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public BookingMessageResponse send(Long bookingId, Long userId, BookingMessageRequest request) {
        Booking booking = findBooking(bookingId);
        validateParticipant(booking, userId);

        BookingMessage message = new BookingMessage();
        message.setBooking(booking);

        User sender = new User();
        sender.setId(userId);
        message.setSender(sender);

        message.setMessage(request.getMessage().trim());
        BookingMessage saved = messageRepository.save(message);
        BookingMessageResponse response = mapper.toResponse(saved);
        bookingRealtimeService.publishMessage(response);
        return response;
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    private void validateParticipant(Booking booking, Long userId) {
        boolean isUser = booking.getUser() != null && booking.getUser().getId().equals(userId);
        boolean isProvider = booking.getProvider() != null && booking.getProvider().getId().equals(userId);
        if (!isUser && !isProvider) {
            throw new BadRequestException("You are not authorized to access this booking chat");
        }
    }
}
