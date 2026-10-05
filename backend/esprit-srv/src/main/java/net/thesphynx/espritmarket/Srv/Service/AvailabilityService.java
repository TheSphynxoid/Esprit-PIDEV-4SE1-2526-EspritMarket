package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Srv.Dto.TimeSlotDto;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Entity.ProviderExceptionType;
import net.thesphynx.espritmarket.Srv.Entity.ProviderWeeklyTemplate;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProviderExceptionRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProviderMandateRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProviderWeeklyTemplateRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceMandateRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@org.springframework.stereotype.Service
public class AvailabilityService {
    private static final Set<BookingStatus> ACTIVE_BOOKING_STATUSES = Set.of(
            BookingStatus.PENDING, BookingStatus.PENDING_EVALUATION, BookingStatus.TENTATIVE,
            BookingStatus.APPROVED, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS
    );

    private final IProviderWeeklyTemplateRepository templateRepository;
    private final IProviderExceptionRepository exceptionRepository;
    private final IServiceMandateRepository serviceMandateRepository;
    private final IProviderMandateRepository providerMandateRepository;
    private final IBookingRepository bookingRepository;
    private final IServiceRepository serviceRepository;

    public AvailabilityService(IProviderWeeklyTemplateRepository templateRepository,
                               IProviderExceptionRepository exceptionRepository,
                               IServiceMandateRepository serviceMandateRepository,
                               IProviderMandateRepository providerMandateRepository,
                               IBookingRepository bookingRepository,
                               IServiceRepository serviceRepository) {
        this.templateRepository = templateRepository;
        this.exceptionRepository = exceptionRepository;
        this.serviceMandateRepository = serviceMandateRepository;
        this.providerMandateRepository = providerMandateRepository;
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
    }

    public List<TimeSlotDto> getAvailableSlots(Long serviceId, LocalDate date) {
        return getAvailableSlots(serviceId, date, date);
    }

    public List<TimeSlotDto> getAvailableSlots(Long serviceId, LocalDate startDate, LocalDate endDate) {
        net.thesphynx.espritmarket.Srv.Entity.Service service = serviceRepository.findById(serviceId)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));

        if (service.getProvider() == null) {
            throw new BadRequestException("Service has no assigned provider");
        }

        Long providerId = service.getProvider().getId();
        List<ProviderWeeklyTemplate> templates = templateRepository.findTemplatesForProviderService(providerId, serviceId);

        if (templates.isEmpty()) {
            templates = templateRepository.findByProviderIdAndServiceIdIsNull(providerId);
        }

        List<TimeSlotDto> allSlots = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            boolean isBlocked = exceptionRepository.findByProviderIdAndDate(providerId, date).stream()
                    .anyMatch(e -> e.getType() == ProviderExceptionType.BLOCKED);

            if (isBlocked) continue;

            DayOfWeek dayOfWeek = date.getDayOfWeek();
            List<ProviderWeeklyTemplate> dayTemplates = templates.stream()
                    .filter(t -> t.getDayOfWeek() == dayOfWeek)
                    .toList();

            for (ProviderWeeklyTemplate template : dayTemplates) {
                List<TimeSlotDto> daySlots = generateSlotsForTemplate(template, date, providerId, serviceId);
                allSlots.addAll(daySlots);
            }
        }

        Map<LocalDateTime, TimeSlotDto> deduped = new LinkedHashMap<>();
        for (TimeSlotDto slot : allSlots) {
            deduped.merge(slot.getStart(), slot, (existing, incoming) ->
                    incoming.getAvailableCapacity() > existing.getAvailableCapacity() ? incoming : existing);
        }

        return new ArrayList<>(deduped.values());
    }

    @Transactional
    public void validateBookingAvailability(Long serviceId, LocalDateTime slotStart, double durationHours) {
        net.thesphynx.espritmarket.Srv.Entity.Service service = serviceRepository.findById(serviceId)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));

        if (service.getProvider() == null) {
            throw new BadRequestException("Service has no assigned provider");
        }

        Long providerId = service.getProvider().getId();
        long durationMinutes = (long) (durationHours * 60);
        if (durationMinutes <= 0) {
            throw new BadRequestException("Booking duration must be positive");
        }
        LocalDateTime slotEnd = slotStart.plusMinutes(durationMinutes);
        LocalDate date = slotStart.toLocalDate();

        boolean isBlocked = exceptionRepository.findByProviderIdAndDate(providerId, date).stream()
                .anyMatch(e -> e.getType() == ProviderExceptionType.BLOCKED);
        if (isBlocked) {
            throw new BadRequestException("The provider is not available on the selected date");
        }

        if (service.getPricingType() == net.thesphynx.espritmarket.Srv.Entity.PricingType.FIXED) {
            enforceServiceMandate(providerId, serviceId);
            enforceProviderMandate(providerId);
            return;
        }

        List<ProviderWeeklyTemplate> templates = templateRepository.findTemplatesForProviderService(providerId, serviceId);
        if (templates.isEmpty()) {
            templates = templateRepository.findByProviderIdAndServiceIdIsNull(providerId);
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        int maxConcurrent = 0;
        boolean withinTemplate = false;
        int matchedSlotDuration = 60;

        for (ProviderWeeklyTemplate template : templates) {
            if (template.getDayOfWeek() != dayOfWeek) continue;

            LocalTime tStart = template.getStartHour();
            LocalTime tEnd = template.getEndHour();

            List<net.thesphynx.espritmarket.Srv.Entity.ProviderException> exceptions =
                    exceptionRepository.findByProviderIdAndDate(providerId, date);
            for (net.thesphynx.espritmarket.Srv.Entity.ProviderException ex : exceptions) {
                if (ex.getType() == ProviderExceptionType.CUSTOM_HOURS) {
                    if (ex.getStartHour() != null) tStart = ex.getStartHour();
                    if (ex.getEndHour() != null) tEnd = ex.getEndHour();
                }
            }

            LocalDateTime templateStart = LocalDateTime.of(date, tStart);
            LocalDateTime templateEnd = LocalDateTime.of(date, tEnd);
            if (!tEnd.isAfter(tStart)) {
                templateEnd = templateEnd.plusDays(1);
            }

            if (!slotStart.isBefore(templateStart) && !slotEnd.isAfter(templateEnd)) {
                withinTemplate = true;
                maxConcurrent = Math.max(maxConcurrent, template.getMaxConcurrent());
                matchedSlotDuration = template.getSlotDurationMinutes();
            }
        }

        if (!withinTemplate) {
            throw new BadRequestException("The requested time is outside the provider's working hours");
        }

        long totalDurationMinutes = (long) (durationHours * 60);
        if (totalDurationMinutes % matchedSlotDuration != 0) {
            throw new BadRequestException(
                    "Booking duration must be a multiple of " + matchedSlotDuration + " minutes");
        }

        int overlapping = countOverlappingBookings(providerId, serviceId, slotStart, slotEnd);
        if (overlapping >= maxConcurrent) {
            throw new BadRequestException("The requested time slot is already booked");
        }

        enforceServiceMandate(providerId, serviceId);
        enforceProviderMandate(providerId);
    }

    public int countActiveBookingsForService(Long providerId, Long serviceId) {
        return (int) bookingRepository.countActiveByProviderServiceAndStatuses(providerId, serviceId, ACTIVE_BOOKING_STATUSES);
    }

    public int countActiveBookingsForProvider(Long providerId) {
        return (int) bookingRepository.countActiveByProviderAndStatuses(providerId, ACTIVE_BOOKING_STATUSES);
    }

    public boolean isServiceOverbooked(Long providerId, Long serviceId) {
        return serviceMandateRepository.findByProviderIdAndServiceId(providerId, serviceId)
                .map(mandate -> countActiveBookingsForService(providerId, serviceId) >= mandate.getMaxBookings())
                .orElse(false);
    }

    public boolean isProviderOverbooked(Long providerId) {
        return providerMandateRepository.findByProviderId(providerId)
                .map(mandate -> countActiveBookingsForProvider(providerId) >= mandate.getMaxBookings())
                .orElse(false);
    }

    private List<TimeSlotDto> generateSlotsForTemplate(ProviderWeeklyTemplate template, LocalDate date,
                                                        Long providerId, Long serviceId) {
        List<TimeSlotDto> slots = new ArrayList<>();
        LocalTime startHour = template.getStartHour();
        LocalTime endHour = template.getEndHour();

        List<net.thesphynx.espritmarket.Srv.Entity.ProviderException> exceptions =
                exceptionRepository.findByProviderIdAndDate(providerId, date);

        for (net.thesphynx.espritmarket.Srv.Entity.ProviderException ex : exceptions) {
            if (ex.getType() == ProviderExceptionType.CUSTOM_HOURS) {
                startHour = ex.getStartHour() != null ? ex.getStartHour() : startHour;
                endHour = ex.getEndHour() != null ? ex.getEndHour() : endHour;
            }
        }

        int durationMinutes = template.getSlotDurationMinutes();
        LocalTime current = startHour;

        while (current.plusMinutes(durationMinutes).compareTo(endHour) <= 0) {
            LocalDateTime slotStart = LocalDateTime.of(date, current);
            LocalDateTime slotEnd = LocalDateTime.of(date, current.plusMinutes(durationMinutes));

            int currentBookings = countOverlappingBookings(providerId, serviceId, slotStart, slotEnd);
            int maxConcurrent = template.getMaxConcurrent();
            int availableCapacity = maxConcurrent - currentBookings;

            slots.add(new TimeSlotDto(
                    slotStart, slotEnd, maxConcurrent, currentBookings,
                    Math.max(0, availableCapacity), availableCapacity > 0, durationMinutes
            ));

            current = current.plusMinutes(durationMinutes);
        }

        return slots;
    }

    private int countOverlappingBookings(Long providerId, Long serviceId,
                                          LocalDateTime slotStart, LocalDateTime slotEnd) {
        List<String> statusNames = ACTIVE_BOOKING_STATUSES.stream().map(BookingStatus::name).toList();
        return (int) bookingRepository.countOverlappingBookings(
                providerId, serviceId, statusNames, slotStart, slotEnd);
    }

    private void enforceServiceMandate(Long providerId, Long serviceId) {
        serviceMandateRepository.findByProviderIdAndServiceId(providerId, serviceId)
                .ifPresent(mandate -> {
                    int current = countActiveBookingsForService(providerId, serviceId);
                    if (current >= mandate.getMaxBookings()) {
                        throw new BadRequestException(
                                "Service booking limit reached (" + mandate.getMaxBookings() + " max for this service)");
                    }
                });
    }

    private void enforceProviderMandate(Long providerId) {
        providerMandateRepository.findByProviderId(providerId)
                .ifPresent(mandate -> {
                    int current = countActiveBookingsForProvider(providerId);
                    if (current >= mandate.getMaxBookings()) {
                        throw new BadRequestException(
                                "Provider booking limit reached (" + mandate.getMaxBookings() + " max total)");
                    }
                });
    }
}
