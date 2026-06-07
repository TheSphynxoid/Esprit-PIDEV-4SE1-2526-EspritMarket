package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Srv.Dto.SurgePricingResponse;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SurgePricingService {

    private final IBookingRepository bookingRepository;

    public SurgePricingService(IBookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public SurgePricingResponse getSurgeData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thisWeekStart = now.minusDays(7);
        LocalDateTime lastWeekStart = now.minusDays(14);

        List<BookingStatus> activeStatuses = List.of(
                BookingStatus.PENDING, BookingStatus.PENDING_EVALUATION, BookingStatus.TENTATIVE,
                BookingStatus.APPROVED, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS);

        var allBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getDeletedAt() == null && b.getService() != null)
                .toList();

        Map<String, Long> thisWeekByCategory = allBookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(thisWeekStart))
                .collect(Collectors.groupingBy(
                        b -> b.getService().getCategory() != null ? b.getService().getCategory().name() : "OTHER",
                        Collectors.counting()));

        Map<String, Long> lastWeekByCategory = allBookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(lastWeekStart) && b.getCreatedAt().isBefore(thisWeekStart))
                .collect(Collectors.groupingBy(
                        b -> b.getService().getCategory() != null ? b.getService().getCategory().name() : "OTHER",
                        Collectors.counting()));

        List<SurgePricingResponse.CategoryDemand> categories = new ArrayList<>();
        for (String category : thisWeekByCategory.keySet()) {
            int thisWeek = thisWeekByCategory.getOrDefault(category, 0L).intValue();
            int lastWeek = lastWeekByCategory.getOrDefault(category, 0L).intValue();
            double growth = lastWeek > 0 ? ((thisWeek - lastWeek) * 100.0 / lastWeek) : (thisWeek > 0 ? 100.0 : 0.0);

            double demandLevel;
            String badge;
            if (thisWeek >= 10) { demandLevel = 5.0; badge = "CRITICAL"; }
            else if (thisWeek >= 6) { demandLevel = 4.0; badge = "HIGH"; }
            else if (thisWeek >= 3) { demandLevel = 3.0; badge = "MODERATE"; }
            else if (thisWeek >= 1) { demandLevel = 2.0; badge = "LOW"; }
            else { demandLevel = 1.0; badge = "NORMAL"; }

            double multiplier = 1.0;
            if (demandLevel >= 5.0) multiplier = 1.25;
            else if (demandLevel >= 4.0) multiplier = 1.15;
            else if (demandLevel >= 3.0) multiplier = 1.08;

            categories.add(SurgePricingResponse.CategoryDemand.builder()
                    .category(category).bookingsThisWeek(thisWeek).bookingsLastWeek(lastWeek)
                    .growthPercent(Math.round(growth * 10.0) / 10.0).demandLevel(demandLevel)
                    .suggestedMultiplier(multiplier).badge(badge).build());
        }

        categories.sort((a, b) -> Double.compare(b.getDemandLevel(), a.getDemandLevel()));

        Map<Long, List<net.thesphynx.espritmarket.Srv.Entity.Booking>> bookingsByProvider = allBookings.stream()
                .filter(b -> b.getProvider() != null)
                .collect(Collectors.groupingBy(b -> b.getProvider().getId()));

        List<SurgePricingResponse.ProviderDemand> providers = new ArrayList<>();
        for (Map.Entry<Long, List<net.thesphynx.espritmarket.Srv.Entity.Booking>> entry : bookingsByProvider.entrySet()) {
            Long providerId = entry.getKey();
            List<net.thesphynx.espritmarket.Srv.Entity.Booking> providerBookings = entry.getValue();

            int activeBookings = (int) providerBookings.stream()
                    .filter(b -> activeStatuses.contains(b.getStatus()))
                    .count();
            int completedLastWeek = (int) providerBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getUpdatedAt() != null && b.getUpdatedAt().isAfter(thisWeekStart))
                    .count();

            double workload = Math.min(100.0, activeBookings * 20.0);
            double multiplier = 1.0;
            String badge;
            int waitDays = 0;

            if (workload >= 80) { multiplier = 1.20; badge = "OVERLOADED"; waitDays = 7; }
            else if (workload >= 60) { multiplier = 1.12; badge = "HIGH_DEMAND"; waitDays = 4; }
            else if (workload >= 40) { multiplier = 1.05; badge = "MODERATE"; waitDays = 2; }
            else { badge = "AVAILABLE"; waitDays = 1; }

            String providerName = providerBookings.stream()
                    .filter(b -> b.getProvider() != null)
                    .findFirst()
                    .map(b -> b.getProvider().getName())
                    .orElse("Unknown");

            providers.add(SurgePricingResponse.ProviderDemand.builder()
                    .providerId(providerId).providerName(providerName)
                    .activeBookings(activeBookings).completedLastWeek(completedLastWeek)
                    .workloadPercent(workload).suggestedMultiplier(multiplier)
                    .badge(badge).estimatedWaitDays(waitDays).build());
        }

        providers.sort((a, b) -> Double.compare(b.getWorkloadPercent(), a.getWorkloadPercent()));

        return SurgePricingResponse.builder().categories(categories).providers(providers).build();
    }
}
