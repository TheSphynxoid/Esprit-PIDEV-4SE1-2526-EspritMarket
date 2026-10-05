package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
public class ProviderScoringService {

    private static final Set<BookingStatus> TERMINAL_STATUSES = Set.of(
            BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.REJECTED, BookingStatus.DISPUTED
    );

    private static final Collection<BookingStatus> ACTIVE_STATUSES = Set.of(
            BookingStatus.PENDING, BookingStatus.PENDING_EVALUATION, BookingStatus.TENTATIVE,
            BookingStatus.APPROVED, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS
    );

    private final IBookingRepository bookingRepository;

    public ProviderScoringService(IBookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public double computeReliability(Long providerId) {
        long total = bookingRepository.countActiveByProviderAndStatuses(providerId,
                List.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.DISPUTED));

        if (total == 0) return 0.5;

        long completed = bookingRepository.countActiveByProviderAndStatuses(providerId,
                List.of(BookingStatus.COMPLETED));

        double rate = (double) completed / total;
        return clamp(rate);
    }

    public double computeFairness(Long providerId) {
        long activeCount = bookingRepository.countActiveByProviderAndStatuses(providerId, ACTIVE_STATUSES);

        if (activeCount <= 2) return 0.9;
        if (activeCount <= 5) return 0.7;
        if (activeCount <= 10) return 0.5;
        if (activeCount <= 20) return 0.3;
        return 0.15;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
