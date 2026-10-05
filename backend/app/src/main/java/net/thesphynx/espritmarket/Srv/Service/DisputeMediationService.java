package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Srv.Dto.DisputeMediationResponse;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Entity.DisputeMediation;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IDisputeMediationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class DisputeMediationService {

    private final IDisputeMediationRepository mediationRepository;
    private final IBookingRepository bookingRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public DisputeMediationService(IDisputeMediationRepository mediationRepository,
                                    IBookingRepository bookingRepository) {
        this.mediationRepository = mediationRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public DisputeMediationResponse createMediation(Long bookingId) {
        if (mediationRepository.existsByBookingId(bookingId)) {
            return getMediation(bookingId);
        }

        Optional<Booking> optBooking = bookingRepository.findById(bookingId);
        if (optBooking.isEmpty()) {
            throw new IllegalArgumentException("Booking not found");
        }

        Booking booking = optBooking.get();

        DisputeMediation mediation = new DisputeMediation();
        mediation.setBooking(booking);
        mediation.setStatus(DisputeMediation.MediationStatus.IN_PROGRESS);

        AnalysisResult analysis = analyzeDispute(booking);
        mediation.setSuggestedResolution(analysis.resolution);
        mediation.setSuggestedRefundPercent(analysis.refundPercent);
        mediation.setSuggestedDeadlineExtensionDays(analysis.extensionDays);
        mediation.setAnalysisSummary(analysis.summary);

        mediationRepository.save(mediation);
        return toResponse(mediation);
    }

    public Optional<DisputeMediationResponse> getMediationOptional(Long bookingId) {
        return mediationRepository.findByBookingId(bookingId).map(this::toResponse);
    }

    public DisputeMediationResponse getMediation(Long bookingId) {
        return mediationRepository.findByBookingId(bookingId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("No mediation found for booking"));
    }

    @Transactional
    public DisputeMediationResponse vote(Long bookingId, Long userId, String vote) {
        DisputeMediation mediation = mediationRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("No mediation found"));

        if (mediation.getStatus() != DisputeMediation.MediationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Mediation is not in progress");
        }

        DisputeMediation.VoteType voteType;
        try {
            voteType = DisputeMediation.VoteType.valueOf(vote.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid vote: must be ACCEPT, REJECT, or ABSTAIN");
        }

        if (bookingRepository.existsByIdAndUserId(bookingId, userId)) {
            mediation.setClientVote(voteType);
        } else if (bookingRepository.existsByIdAndProviderId(bookingId, userId)) {
            mediation.setProviderVote(voteType);
        } else {
            throw new IllegalArgumentException("User is not part of this booking");
        }

        if (mediation.getClientVote() != null && mediation.getProviderVote() != null) {
            if (mediation.getClientVote() == DisputeMediation.VoteType.ACCEPT
                    && mediation.getProviderVote() == DisputeMediation.VoteType.ACCEPT) {
                mediation.setStatus(DisputeMediation.MediationStatus.RESOLVED);
                mediation.setResolvedAt(LocalDateTime.now());
                Booking optB = bookingRepository.findById(bookingId).orElse(null);
                if (optB != null) {
                    optB.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(optB);
                }
            } else {
                mediation.setStatus(DisputeMediation.MediationStatus.ESCALATED);
            }
        }

        mediationRepository.save(mediation);
        return toResponse(mediation);
    }

    private AnalysisResult analyzeDispute(Booking booking) {
        StringBuilder summary = new StringBuilder("Dispute analysis: ");

        long totalProviderBookings = bookingRepository.countActiveByProviderId(booking.getProvider().getId());
        long completedBookings = bookingRepository.countActiveByProviderAndStatuses(booking.getProvider().getId(),
                List.of(BookingStatus.COMPLETED));
        long disputedBookings = bookingRepository.countActiveByProviderAndStatuses(booking.getProvider().getId(),
                List.of(BookingStatus.DISPUTED));

        double providerCompletionRate = totalProviderBookings > 0 ? (completedBookings * 100.0 / totalProviderBookings) : 0;
        double providerDisputeRate = totalProviderBookings > 0 ? (disputedBookings * 100.0 / totalProviderBookings) : 0;

        if (providerCompletionRate >= 80 && providerDisputeRate <= 10) {
            summary.append("Provider has a strong track record (").append(Math.round(providerCompletionRate)).append("% completion rate). ");
            summary.append("Suggesting deadline extension to allow provider to deliver.");
            return new AnalysisResult(DisputeMediation.ResolutionType.EXTEND_DEADLINE, 0.0, 7, summary.toString());
        }

        if (providerCompletionRate >= 50) {
            summary.append("Provider has moderate track record (").append(Math.round(providerCompletionRate)).append("% completion). ");
            summary.append("Suggesting partial refund of 50% as compromise.");
            return new AnalysisResult(DisputeMediation.ResolutionType.PARTIAL_REFUND, 50.0, 0, summary.toString());
        }

        if (providerDisputeRate >= 30) {
            summary.append("Provider has high dispute rate (").append(Math.round(providerDisputeRate)).append("%). ");
            summary.append("Suggesting full refund due to repeated issues.");
            return new AnalysisResult(DisputeMediation.ResolutionType.FULL_REFUND, 100.0, 0, summary.toString());
        }

        summary.append("Provider performance is below average. ");
        summary.append("Suggesting partial refund of 75%.");
        return new AnalysisResult(DisputeMediation.ResolutionType.PARTIAL_REFUND, 75.0, 0, summary.toString());
    }

    private DisputeMediationResponse toResponse(DisputeMediation m) {
        return DisputeMediationResponse.builder()
                .id(m.getId())
                .bookingId(m.getBooking().getId())
                .suggestedResolution(m.getSuggestedResolution().name())
                .suggestedRefundPercent(m.getSuggestedRefundPercent())
                .suggestedDeadlineExtensionDays(m.getSuggestedDeadlineExtensionDays())
                .analysisSummary(m.getAnalysisSummary())
                .clientVote(m.getClientVote() != null ? m.getClientVote().name() : null)
                .providerVote(m.getProviderVote() != null ? m.getProviderVote().name() : null)
                .status(m.getStatus().name())
                .resolvedAt(m.getResolvedAt() != null ? m.getResolvedAt().format(FMT) : null)
                .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().format(FMT) : null)
                .build();
    }

    private record AnalysisResult(
            DisputeMediation.ResolutionType resolution,
            double refundPercent,
            int extensionDays,
            String summary
    ) {}
}
