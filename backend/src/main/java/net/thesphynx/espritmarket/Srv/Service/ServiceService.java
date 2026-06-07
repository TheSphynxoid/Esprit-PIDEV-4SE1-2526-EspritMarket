package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Srv.Dto.CompatibilityResponse;
import net.thesphynx.espritmarket.Srv.Dto.ProviderStandingResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceComparisonResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceUpsertRequest;
import net.thesphynx.espritmarket.Srv.Dto.ServiceResponse;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Entity.Service;
import net.thesphynx.espritmarket.Srv.Entity.ServiceCategory;
import net.thesphynx.espritmarket.Srv.Entity.ServiceTag;
import net.thesphynx.espritmarket.Srv.Mapper.ServiceMapper;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceReviewRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceTagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@org.springframework.stereotype.Service
public class ServiceService {
    private final IServiceRepository serviceRepository;
    private final IServiceTagRepository serviceTagRepository;
    private final ServiceMapper serviceMapper;
    private final IBookingRepository bookingRepository;
    private final IServiceReviewRepository reviewRepository;

    public ServiceService(IServiceRepository serviceRepository, IServiceTagRepository serviceTagRepository,
                          ServiceMapper serviceMapper, IBookingRepository bookingRepository,
                          IServiceReviewRepository reviewRepository) {
        this.serviceRepository = serviceRepository;
        this.serviceTagRepository = serviceTagRepository;
        this.serviceMapper = serviceMapper;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
    }

    public PageResponse<ServiceResponse> getAll(int page, int size) {
        Page<Service> result = serviceRepository.findAllActive(PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public Optional<ServiceResponse> getById(Long id) {
        return serviceRepository.findById(id)
                .filter(s -> s.getDeletedAt() == null)
                .map(serviceMapper::toResponse);
    }

    public PageResponse<ServiceResponse> getByProviderId(Long providerId, int page, int size) {
        Page<Service> result = serviceRepository.findActiveByProviderId(providerId, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public PageResponse<ServiceResponse> getByCategory(ServiceCategory category, int page, int size) {
        Page<Service> result = serviceRepository.findByCategory(category, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public PageResponse<ServiceResponse> search(String keyword, int page, int size) {
        Page<Service> result = serviceRepository.searchByName(keyword, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public PageResponse<ServiceResponse> getByFilters(ServiceCategory category, BigDecimal minPrice,
                                                       BigDecimal maxPrice, String location, int page, int size) {
        Page<Service> result = serviceRepository.findByFilters(category, minPrice, maxPrice, location, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    @Transactional
    public ServiceResponse create(ServiceUpsertRequest request) {
        sanitizeImageUrl(request);
        Service entity = serviceMapper.toEntity(request);
        Service saved = serviceRepository.save(entity);
        if (request.getTags() != null) {
            saveTags(saved, request.getTags());
        }
        return serviceMapper.toResponse(serviceRepository.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    public ServiceResponse update(Long id, ServiceUpsertRequest request) {
        sanitizeImageUrl(request);
        Service existing = serviceRepository.findById(id)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        Service entity = serviceMapper.toEntity(request);
        entity.setId(id);
        entity.setDeletedAt(existing.getDeletedAt());
        Service saved = serviceRepository.save(entity);
        serviceTagRepository.deleteByServiceId(id);
        if (request.getTags() != null) {
            saveTags(saved, request.getTags());
        }
        return serviceMapper.toResponse(serviceRepository.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    public ServiceResponse updateProjectParticipation(Long id, boolean allowProjectParticipation) {
        Service service = serviceRepository.findById(id)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        service.setAllowProjectParticipation(allowProjectParticipation);
        return serviceMapper.toResponse(serviceRepository.save(service));
    }

    @Transactional
    public int updateProviderProjectParticipation(Long providerId, boolean allowProjectParticipation) {
        List<Service> services = serviceRepository.findAllActive(PageRequest.of(0, 500)).getContent()
                .stream()
                .filter(s -> s.getProvider() != null && providerId.equals(s.getProvider().getId()))
                .toList();

        services.forEach(s -> s.setAllowProjectParticipation(allowProjectParticipation));
        serviceRepository.saveAll(services);
        return services.size();
    }

    @Transactional
    public void delete(Long id) {
        serviceRepository.findById(id).ifPresent(service -> {
            service.setDeletedAt(java.time.LocalDateTime.now());
            serviceRepository.save(service);
        });
    }

    private void saveTags(Service service, List<String> tags) {
        for (String tag : tags) {
            ServiceTag serviceTag = new ServiceTag();
            serviceTag.setService(service);
            serviceTag.setTag(tag.trim().toLowerCase());
            serviceTagRepository.save(serviceTag);
        }
    }

    public ProviderStandingResponse getProviderStanding(Long providerId) {
        ProviderStandingResponse resp = new ProviderStandingResponse();
        resp.setProviderId(providerId);

        long total = bookingRepository.countActiveByProviderId(providerId);
        long completed = bookingRepository.countActiveByProviderAndStatuses(providerId,
                List.of(BookingStatus.COMPLETED));
        long cancelled = bookingRepository.countActiveByProviderAndStatuses(providerId,
                List.of(BookingStatus.CANCELLED));
        long disputed = bookingRepository.countActiveByProviderAndStatuses(providerId,
                List.of(BookingStatus.DISPUTED));
        long active = bookingRepository.countActiveByProviderAndStatuses(providerId,
                List.of(BookingStatus.PENDING, BookingStatus.PENDING_EVALUATION, BookingStatus.TENTATIVE,
                        BookingStatus.APPROVED, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS));

        long reviewCount = reviewRepository.countByProviderId(providerId);
        Double avgRating = reviewRepository.averageRatingByProviderId(providerId);

        long activeServices = serviceRepository.findAllActive(PageRequest.of(0, 500)).getContent().stream()
                .filter(s -> s.getProvider() != null && providerId.equals(s.getProvider().getId()))
                .count();

        resp.setTotalBookings(total);
        resp.setCompletedBookings(completed);
        resp.setCancelledBookings(cancelled);
        resp.setDisputedBookings(disputed);
        resp.setActiveBookings(active);
        resp.setCompletionRate(total > 0 ? BigDecimal.valueOf(completed * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0);
        resp.setCancellationRate(total > 0 ? BigDecimal.valueOf(cancelled * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0);
        resp.setDisputeRate(total > 0 ? BigDecimal.valueOf(disputed * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0);
        resp.setTotalReviews(reviewCount);
        resp.setAverageRating(avgRating != null ? BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP).doubleValue() : null);
        resp.setActiveServices(activeServices);

        List<Service> providerSvcs = serviceRepository.findAllActive(PageRequest.of(0, 500)).getContent().stream()
                .filter(s -> s.getProvider() != null && providerId.equals(s.getProvider().getId()))
                .limit(1)
                .toList();
        if (!providerSvcs.isEmpty()) {
            resp.setProviderName(providerSvcs.get(0).getProvider().getName());
        }

        long xp = (completed * 10L) + (reviewCount * 5L) - (disputed * 15L);
        xp = Math.max(0, xp);
        resp.setXp(xp);

        String level;
        int levelNumber;
        if (xp >= 500) { level = "TOP_RATED"; levelNumber = 4; }
        else if (xp >= 200) { level = "PRO"; levelNumber = 3; }
        else if (xp >= 50) { level = "RISING"; levelNumber = 2; }
        else { level = "NEW"; levelNumber = 1; }
        resp.setLevel(level);
        resp.setLevelNumber(levelNumber);

        return resp;
    }

    public List<Service> getProviderActiveServices(Long providerId) {
        return serviceRepository.findAllActive(PageRequest.of(0, 500)).getContent().stream()
                .filter(s -> s.getProvider() != null && providerId.equals(s.getProvider().getId()))
                .toList();
    }

    private PageResponse<ServiceResponse> toPageResponse(Page<Service> page) {
        List<ServiceResponse> content = page.getContent().stream()
                .map(serviceMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private static final Set<String> ALLOWED_IMAGE_SCHEMES = Set.of("https");
    private static final Set<String> ALLOWED_IMAGE_HOSTS = Set.of(
            "images.unsplash.com", "res.cloudinary.com", "ui-avatars.com",
            "lh3.googleusercontent.com", "platform-lookaside.fbsbx.com",
            "scontent", "twimg.com", "pbs.twimg.com"
    );

    private void sanitizeImageUrl(ServiceUpsertRequest request) {
        String url = request.getImageUrl();
        if (url == null || url.isBlank()) {
            return;
        }
        url = url.trim();
        if (url.startsWith("/api/") || url.startsWith("/uploads/")) {
            request.setImageUrl(url);
            return;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || !"https".equals(scheme)) {
                request.setImageUrl(null);
                return;
            }
            if (host == null || !isAllowedImageHost(host)) {
                request.setImageUrl(null);
                return;
            }
            String path = uri.getPath();
            if (path == null || !path.matches(".*\\.(jpg|jpeg|png|gif|webp|svg)(\\?.*)?$")) {
                request.setImageUrl(null);
                return;
            }
            request.setImageUrl(url);
        } catch (URISyntaxException e) {
            request.setImageUrl(null);
        }
    }

    private boolean isAllowedImageHost(String host) {
        if (host == null) return false;
        String lower = host.toLowerCase();
        for (String allowed : ALLOWED_IMAGE_HOSTS) {
            if (lower.equals(allowed) || lower.endsWith("." + allowed)) {
                return true;
            }
        }
        return false;
    }

    public PageResponse<ServiceResponse> getRelated(Long serviceId, int page, int size) {
        Service svc = serviceRepository.findById(serviceId)
                .filter(s -> s.getDeletedAt() == null)
                .orElse(null);
        if (svc == null || svc.getCategory() == null) {
            return toPageResponse(serviceRepository.findAllActive(PageRequest.of(page, size)));
        }
        Page<Service> related = serviceRepository.findByCategory(svc.getCategory(), PageRequest.of(page, size));
        return toPageResponse(related);
    }

    public CompatibilityResponse getCompatibility(Long providerId, Long clientId) {
        var provider = serviceRepository.findById(providerId).map(s -> s.getProvider()).orElse(null);
        if (provider == null) {
            return CompatibilityResponse.builder().providerId(providerId).clientId(clientId).overallScore(0).matchLevel("NONE").factors(List.of()).build();
        }

        List<CompatibilityResponse.CompatibilityFactor> factors = new ArrayList<>();
        double totalWeight = 0;
        double weightedSum = 0;

        long sharedBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getService() != null && b.getService().getProvider() != null
                        && b.getService().getProvider().getId().equals(providerId)
                        && b.getUser() != null && b.getUser().getId().equals(clientId))
                .count();
        double pastFactor = Math.min(sharedBookings * 20, 100);
        factors.add(CompatibilityResponse.CompatibilityFactor.builder().name("Past Interactions").score(pastFactor).weight(25)
                .detail(sharedBookings + " previous booking(s)").build());
        totalWeight += 25;
        weightedSum += pastFactor * 25;

        long providerActiveServices = getProviderActiveServices(providerId).size();
        double activityFactor = Math.min(providerActiveServices * 20, 100);
        factors.add(CompatibilityResponse.CompatibilityFactor.builder().name("Provider Activity").score(activityFactor).weight(20)
                .detail(providerActiveServices + " active service(s)").build());
        totalWeight += 20;
        weightedSum += activityFactor * 20;

        var reviews = reviewRepository.findByBooking_Service_Provider_IdAndBooking_Status(providerId,
                net.thesphynx.espritmarket.Srv.Entity.BookingStatus.COMPLETED,
                org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
        double avgRating = reviews.isEmpty() ? 0 : reviews.stream().mapToDouble(r -> r.getRating() != null ? r.getRating().doubleValue() : 0).average().orElse(0);
        double ratingFactor = (avgRating / 5.0) * 100;
        factors.add(CompatibilityResponse.CompatibilityFactor.builder().name("Provider Rating").score(ratingFactor).weight(30)
                .detail(String.format("%.1f/5.0 (%d reviews)", avgRating, reviews.size())).build());
        totalWeight += 30;
        weightedSum += ratingFactor * 30;

        long completedBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getService() != null && b.getService().getProvider() != null
                        && b.getService().getProvider().getId().equals(providerId)
                        && b.getStatus() == net.thesphynx.espritmarket.Srv.Entity.BookingStatus.COMPLETED)
                .count();
        long totalProviderBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getService() != null && b.getService().getProvider() != null
                        && b.getService().getProvider().getId().equals(providerId))
                .count();
        double completionFactor = totalProviderBookings > 0 ? (completedBookings * 100.0 / totalProviderBookings) : 50;
        factors.add(CompatibilityResponse.CompatibilityFactor.builder().name("Completion Rate").score(completionFactor).weight(25)
                .detail(String.format("%.0f%% (%d/%d)", completionFactor, completedBookings, totalProviderBookings)).build());
        totalWeight += 25;
        weightedSum += completionFactor * 25;

        double overall = totalWeight > 0 ? weightedSum / totalWeight : 0;
        overall = Math.round(overall * 10.0) / 10.0;
        String matchLevel;
        if (overall >= 80) matchLevel = "EXCELLENT";
        else if (overall >= 60) matchLevel = "GOOD";
        else if (overall >= 40) matchLevel = "FAIR";
        else matchLevel = "LOW";

        return CompatibilityResponse.builder()
                .providerId(providerId)
                .providerName(provider.getName())
                .clientId(clientId)
                .overallScore(overall)
                .matchLevel(matchLevel)
                .factors(factors)
                .build();
    }

    public List<ServiceComparisonResponse> compareServices(List<Long> serviceIds) {
        List<ServiceComparisonResponse> results = new ArrayList<>();

        for (Long serviceId : serviceIds) {
            Optional<Service> opt = serviceRepository.findById(serviceId).filter(s -> s.getDeletedAt() == null);
            if (opt.isEmpty()) continue;
            Service svc = opt.get();

            Long providerId = svc.getProvider() != null ? svc.getProvider().getId() : null;
            String providerName = svc.getProvider() != null ? svc.getProvider().getName() : "Unknown";

            long total = 0;
            long completed = 0;
            double completionRate = 0.0;
            long reviewCount = 0;
            Double avgRating = null;
            String level = "NEW";
            int levelNumber = 1;
            long xp = 0;

            if (providerId != null) {
                total = bookingRepository.countActiveByProviderId(providerId);
                completed = bookingRepository.countActiveByProviderAndStatuses(providerId, List.of(BookingStatus.COMPLETED));
                completionRate = total > 0 ? BigDecimal.valueOf(completed * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0;
                reviewCount = reviewRepository.countByProviderId(providerId);
                avgRating = reviewRepository.averageRatingByProviderId(providerId);
                long disputed = bookingRepository.countActiveByProviderAndStatuses(providerId, List.of(BookingStatus.DISPUTED));
                xp = (completed * 10L) + (reviewCount * 5L) - (disputed * 15L);
                xp = Math.max(0, xp);
                if (xp >= 500) { level = "TOP_RATED"; levelNumber = 4; }
                else if (xp >= 200) { level = "PRO"; levelNumber = 3; }
                else if (xp >= 50) { level = "RISING"; levelNumber = 2; }
            }

            List<String> tags = serviceTagRepository.findByServiceId(serviceId).stream()
                    .map(ServiceTag::getTag).toList();

            int packageCount = svc.getPackages() != null ? svc.getPackages().size() : 0;

            ServiceComparisonResponse item = ServiceComparisonResponse.builder()
                    .serviceId(svc.getId())
                    .serviceName(svc.getName())
                    .category(svc.getCategory() != null ? svc.getCategory().name() : "N/A")
                    .pricingType(svc.getPricingType())
                    .price(svc.getPrice())
                    .startingPrice(svc.getPrice())
                    .description(svc.getDescription())
                    .providerId(providerId)
                    .providerName(providerName)
                    .averageRating(avgRating != null ? BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP).doubleValue() : null)
                    .totalReviews(reviewCount)
                    .totalBookings(total)
                    .completedBookings(completed)
                    .completionRate(completionRate)
                    .level(level)
                    .levelNumber(levelNumber)
                    .mlReliabilityScore(0.0)
                    .mlRiskLevel("UNKNOWN")
                    .packageCount(packageCount)
                    .tags(tags)
                    .build();

            results.add(item);
        }

        return results;
    }
}
