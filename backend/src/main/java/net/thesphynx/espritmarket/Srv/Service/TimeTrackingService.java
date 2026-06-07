package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.TimeTrackingResponse;
import net.thesphynx.espritmarket.Srv.Dto.TimeTrackingResponse.TimeLogEntry;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.TimeLog;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.ITimeLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TimeTrackingService {

    private final ITimeLogRepository timeLogRepository;
    private final IBookingRepository bookingRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public TimeTrackingService(ITimeLogRepository timeLogRepository,
                                IBookingRepository bookingRepository,
                                UserRepository userRepository) {
        this.timeLogRepository = timeLogRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TimeTrackingResponse startTimer(Long bookingId, Long userId, String description) {
        List<TimeLog> activeTimers = timeLogRepository.findActiveByBookingId(bookingId);
        for (TimeLog active : activeTimers) {
            if (active.getUser().getId().equals(userId)) {
                return getTimeTracking(bookingId);
            }
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TimeLog timeLog = new TimeLog();
        timeLog.setBooking(booking);
        timeLog.setUser(user);
        timeLog.setStartTime(LocalDateTime.now());
        timeLog.setDescription(description);
        timeLogRepository.save(timeLog);

        return getTimeTracking(bookingId);
    }

    @Transactional
    public TimeTrackingResponse stopTimer(Long bookingId, Long userId) {
        List<TimeLog> activeTimers = timeLogRepository.findActiveByBookingId(bookingId);
        for (TimeLog active : activeTimers) {
            if (active.getUser().getId().equals(userId)) {
                active.setEndTime(LocalDateTime.now());
                long minutes = Duration.between(active.getStartTime(), active.getEndTime()).toMinutes();
                active.setDurationMinutes((int) minutes);
                timeLogRepository.save(active);
            }
        }
        return getTimeTracking(bookingId);
    }

    public TimeTrackingResponse getTimeTracking(Long bookingId) {
        List<TimeLog> logs = timeLogRepository.findByBookingIdOrderByStartTimeDesc(bookingId);
        Long totalMinutes = timeLogRepository.totalMinutesByBookingId(bookingId);
        if (totalMinutes == null) totalMinutes = 0L;

        TimeLogEntry activeEntry = null;
        List<TimeLogEntry> entries = new ArrayList<>();
        for (TimeLog tl : logs) {
            TimeLogEntry entry = TimeLogEntry.builder()
                    .id(tl.getId())
                    .startTime(tl.getStartTime().format(FMT))
                    .endTime(tl.getEndTime() != null ? tl.getEndTime().format(FMT) : null)
                    .durationMinutes(tl.getDurationMinutes())
                    .description(tl.getDescription())
                    .active(tl.isActive())
                    .build();
            entries.add(entry);
            if (tl.isActive()) activeEntry = entry;
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        long estimatedMinutes = 0;
        if (booking != null && booking.getDuration() > 0) {
            estimatedMinutes = (long) (booking.getDuration() * 60);
        }

        return TimeTrackingResponse.builder()
                .bookingId(bookingId)
                .entries(entries)
                .totalMinutes(totalMinutes)
                .hasActiveTimer(activeEntry != null)
                .activeTimer(activeEntry)
                .estimatedMinutes(estimatedMinutes)
                .build();
    }
}
