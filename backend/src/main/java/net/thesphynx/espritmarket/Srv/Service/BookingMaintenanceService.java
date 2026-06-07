package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(BookingMaintenanceService.class);
    private static final long TENTATIVE_EXPIRY_HOURS = 24;
    private static final long STALE_PENDING_HOURS = 72;
    private static final long AUTO_CONFIRM_APPROVED_HOURS = 12;
    private static final long AUTO_REJECT_PENDING_HOURS = 48;

    private final IBookingRepository bookingRepository;

    public BookingMaintenanceService(IBookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void expireTentativeHolds() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(TENTATIVE_EXPIRY_HOURS);
        List<Booking> expired = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.TENTATIVE, cutoff);
        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.CANCELLED);
            log.info("Tentative hold expired: bookingId={}, serviceId={}", booking.getId(), booking.getService().getId());
        }
        if (!expired.isEmpty()) {
            bookingRepository.saveAll(expired);
        }
    }

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
    @Transactional
    public void cleanupStalePendingEvaluations() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(STALE_PENDING_HOURS);
        List<Booking> stale = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING_EVALUATION, cutoff);
        for (Booking booking : stale) {
            booking.setStatus(BookingStatus.CANCELLED);
            log.info("Stale pending-evaluation cleaned up: bookingId={}", booking.getId());
        }
        if (!stale.isEmpty()) {
            bookingRepository.saveAll(stale);
        }
    }

    @Scheduled(fixedRateString = "${srv.scheduler.auto-confirm-approved.rate-ms:3600000}")
    @Transactional
    public void autoConfirmOldApprovedBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(AUTO_CONFIRM_APPROVED_HOURS);
        List<Booking> approved = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.APPROVED, cutoff);
        for (Booking booking : approved) {
            booking.setStatus(BookingStatus.CONFIRMED);
            log.info("Auto-confirmed old approved booking: bookingId={}", booking.getId());
        }
        if (!approved.isEmpty()) {
            bookingRepository.saveAll(approved);
        }
    }

    @Scheduled(fixedRateString = "${srv.scheduler.auto-reject-pending.rate-ms:3600000}")
    @Transactional
    public void autoRejectOldPendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(AUTO_REJECT_PENDING_HOURS);
        List<Booking> pending = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING, cutoff);
        for (Booking booking : pending) {
            booking.setStatus(BookingStatus.REJECTED);
            log.info("Auto-rejected old pending booking: bookingId={}", booking.getId());
        }
        if (!pending.isEmpty()) {
            bookingRepository.saveAll(pending);
        }
    }
}
