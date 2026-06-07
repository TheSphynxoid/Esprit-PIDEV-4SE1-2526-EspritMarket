package net.thesphynx.espritmarket.Srv.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.thesphynx.espritmarket.Srv.Dto.BookingPredictionResponse;
import net.thesphynx.espritmarket.Srv.Dto.ProjectDelayPredictionResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceRiskAnalysisResponse;
import net.thesphynx.espritmarket.Srv.Entity.*;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectMilestoneRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MlPredictionService {

    private static final Logger log = LoggerFactory.getLogger(MlPredictionService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final IBookingRepository bookingRepository;
    private final IProjectRepository projectRepository;
    private final IProjectMilestoneRepository milestoneRepository;
    private final IServiceReviewRepository reviewRepository;

    @Value("${ml.service.url:http://ml-service:8000}")
    private String mlServiceUrl;

    public MlPredictionService(IBookingRepository bookingRepository,
                               IProjectRepository projectRepository,
                               IProjectMilestoneRepository milestoneRepository,
                               IServiceReviewRepository reviewRepository) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.bookingRepository = bookingRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.reviewRepository = reviewRepository;
    }

    public BookingPredictionResponse predictBookingCompletion(Booking booking) {
        try {
            Map<String, Object> body = buildBookingFeatures(booking);
            String response = restTemplate.postForObject(
                    mlServiceUrl + "/predict/booking", body, String.class);
            return objectMapper.readValue(response, BookingPredictionResponse.class);
        } catch (Exception e) {
            log.warn("ML prediction failed for bookingId={}, using fallback: {}", booking.getId(), e.getMessage());
            return fallbackBookingPrediction(booking);
        }
    }

    public ProjectDelayPredictionResponse predictProjectDelay(Project project) {
        try {
            Map<String, Object> body = buildProjectFeatures(project);
            String response = restTemplate.postForObject(
                    mlServiceUrl + "/predict/project", body, String.class);
            return objectMapper.readValue(response, ProjectDelayPredictionResponse.class);
        } catch (Exception e) {
            log.warn("ML prediction failed for projectId={}, using fallback: {}", project.getId(), e.getMessage());
            return fallbackProjectPrediction(project);
        }
    }

    public ServiceRiskAnalysisResponse analyzeServiceRisks(Project project) {
        ServiceRiskAnalysisResponse response = new ServiceRiskAnalysisResponse();
        response.setProjectId(project.getId());

        List<ProjectMilestone> milestones = milestoneRepository
                .findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(project.getId());

        Map<String, Integer> categoryCancelCounts = new LinkedHashMap<>();
        Map<String, Integer> categoryTotalCounts = new LinkedHashMap<>();

        List<ServiceRiskAnalysisResponse.ServiceRiskItem> items = new java.util.ArrayList<>();

        try {
            for (ProjectMilestone ms : milestones) {
                Set<net.thesphynx.espritmarket.Srv.Entity.Service> services = ms.getServices();
                if (services == null) continue;

                for (net.thesphynx.espritmarket.Srv.Entity.Service svc : services) {
                    BookingPredictionResponse pred = predictBookingCompletionForService(svc);

                    ServiceRiskAnalysisResponse.ServiceRiskItem item = new ServiceRiskAnalysisResponse.ServiceRiskItem();
                    item.setServiceId(svc.getId());
                    item.setServiceName(svc.getName());
                    item.setCategory(svc.getCategory() != null ? svc.getCategory().name() : "OTHER");
                    item.setProviderName(svc.getProvider() != null ? svc.getProvider().getName() : "Unknown");
                    item.setProviderId(svc.getProvider() != null ? svc.getProvider().getId() : null);
                    item.setMilestoneTitle(ms.getTitle());
                    item.setMilestoneId(ms.getId());
                    item.setCompletionProbability(pred.getCompletionProbability());
                    item.setRiskLevel(mapRiskToLevel(pred.getCompletionProbability()));
                    item.setConfidence(pred.getConfidence());
                    item.setRecommendation(pred.getRecommendation());
                    item.setKeyFactors(pred.getKeyFactors());
                    items.add(item);

                    String cat = item.getCategory();
                    categoryTotalCounts.merge(cat, 1, Integer::sum);
                    if (pred.getCompletionProbability() < 0.6) {
                        categoryCancelCounts.merge(cat, 1, Integer::sum);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Service risk analysis partially failed for projectId={}: {}", project.getId(), e.getMessage());
        }

        response.setServices(items);
        return response;
    }

    public List<Map<String, Object>> analyzeSentiment(List<String> texts) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("texts", texts);
            String response = restTemplate.postForObject(
                    mlServiceUrl + "/predict/sentiment", body, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");
            List<Map<String, Object>> items = new ArrayList<>();
            if (results != null && results.isArray()) {
                for (JsonNode r : results) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("sentiment", r.path("sentiment").asText("NEUTRAL"));
                    item.put("confidence", r.path("confidence").asDouble(0.0));
                    item.put("positiveProbability", r.path("positiveProbability").asDouble(0.0));
                    item.put("negativeProbability", r.path("negativeProbability").asDouble(0.0));
                    item.put("neutralProbability", r.path("neutralProbability").asDouble(0.0));
                    items.add(item);
                }
            }
            return items;
        } catch (Exception e) {
            log.warn("ML sentiment analysis failed: {}", e.getMessage());
            return texts.stream().map(t -> {
                Map<String, Object> fallback = new LinkedHashMap<>();
                fallback.put("sentiment", "NEUTRAL");
                fallback.put("confidence", 0.0);
                fallback.put("positiveProbability", 0.33);
                fallback.put("negativeProbability", 0.33);
                fallback.put("neutralProbability", 0.33);
                return fallback;
            }).toList();
        }
    }

    @Async
    public void recordSentimentOutcome(String text, int sentimentLabel) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("modelType", "SENTIMENT");
            body.put("features", Map.of("text", text));
            body.put("label", sentimentLabel);
            restTemplate.postForObject(mlServiceUrl + "/data/record", body, String.class);
        } catch (Exception e) {
            log.warn("Failed to record sentiment outcome: {}", e.getMessage());
        }
    }

    private Map<String, Object> getCategoryRiskStats() {
        return new LinkedHashMap<>(Map.of(
                "categoryCancelCounts", new LinkedHashMap<>(),
                "categoryTotalCounts", new LinkedHashMap<>()
        ));
    }

    public BookingPredictionResponse predictBookingCompletionForService(net.thesphynx.espritmarket.Srv.Entity.Service svc) {
        Map<String, Object> features = new LinkedHashMap<>();

        features.put("serviceCategory", svc.getCategory() != null ? svc.getCategory().name() : "OTHER");

        double providerRating = 0.0;
        int providerCompleted = 0;
        int providerCancelled = 0;
        if (svc.getProvider() != null) {
            List<Booking> providerBookings = bookingRepository.findByProviderIdAndStatusIn(
                    svc.getProvider().getId(), List.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.DISPUTED));
            for (Booking b : providerBookings) {
                if (b.getStatus() == BookingStatus.COMPLETED) providerCompleted++;
                else providerCancelled++;
            }
            Double avgRating = reviewRepository.averageRatingByProviderId(svc.getProvider().getId());
            providerRating = avgRating != null ? avgRating : 0.0;
        }
        features.put("providerRating", providerRating);
        features.put("duration", 2.0);
        features.put("totalPrice", svc.getPrice() != null ? svc.getPrice().doubleValue() : 50.0);
        features.put("dayOfWeek", 2);
        features.put("hour", 10);
        features.put("providerCompletedCount", providerCompleted);
        features.put("providerCancelledCount", providerCancelled);

        try {
            String resp = restTemplate.postForObject(mlServiceUrl + "/predict/booking", features, String.class);
            return objectMapper.readValue(resp, BookingPredictionResponse.class);
        } catch (Exception e) {
            BookingPredictionResponse fallback = new BookingPredictionResponse();
            fallback.setCompletionProbability(0.7);
            fallback.setRiskLevel("MEDIUM");
            fallback.setConfidence("LOW");
            fallback.setKeyFactors(List.of("ML service unavailable"));
            fallback.setRecommendation("Unable to assess service risk.");
            return fallback;
        }
    }

    private int mapRiskToLevel(double probability) {
        if (probability >= 0.8) return 1;
        if (probability >= 0.6) return 2;
        if (probability >= 0.4) return 3;
        return 4;
    }

    @Async
    public void recordBookingOutcome(Booking booking) {
        if (booking.getStatus() != BookingStatus.COMPLETED
                && booking.getStatus() != BookingStatus.CANCELLED
                && booking.getStatus() != BookingStatus.DISPUTED) {
            return;
        }

        try {
            Map<String, Object> features = buildBookingFeatures(booking);
            int label = (booking.getStatus() == BookingStatus.COMPLETED) ? 1 : 0;

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("modelType", "BOOKING");
            body.put("features", features);
            body.put("label", label);

            restTemplate.postForObject(mlServiceUrl + "/data/record", body, String.class);
            log.info("Recorded booking outcome for bookingId={}, label={}", booking.getId(), label);
        } catch (Exception e) {
            log.warn("Failed to record booking outcome for bookingId={}: {}", booking.getId(), e.getMessage());
        }
    }

    @Async
    public void recordProjectSnapshot(Project project) {
        if (project.getStatus() == ProjectStatus.PLANNED) {
            return;
        }

        try {
            Map<String, Object> features = buildProjectFeatures(project);
            int label = (project.getStatus() == ProjectStatus.COMPLETED) ? 1 : 0;

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("modelType", "PROJECT");
            body.put("features", features);
            body.put("label", label);

            restTemplate.postForObject(mlServiceUrl + "/data/record", body, String.class);
            log.info("Recorded project snapshot for projectId={}, label={}", project.getId(), label);
        } catch (Exception e) {
            log.warn("Failed to record project snapshot for projectId={}: {}", project.getId(), e.getMessage());
        }
    }

    private Map<String, Object> buildBookingFeatures(Booking booking) {
        Map<String, Object> features = new LinkedHashMap<>();

        String category = "OTHER";
        if (booking.getService() != null) {
            category = booking.getService().getCategory() != null
                    ? booking.getService().getCategory().name() : "OTHER";
        }
        features.put("serviceCategory", category);

        double providerRating = 0.0;
        int providerCompleted = 0;
        int providerCancelled = 0;
        if (booking.getProvider() != null) {
            List<Booking> providerBookings = bookingRepository.findByProviderIdAndStatusIn(
                    booking.getProvider().getId(), List.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.DISPUTED));
            for (Booking b : providerBookings) {
                if (b.getStatus() == BookingStatus.COMPLETED) providerCompleted++;
                else providerCancelled++;
            }
            Double avgRating = reviewRepository.averageRatingByProviderId(booking.getProvider().getId());
            providerRating = avgRating != null ? avgRating : 0.0;
        }
        features.put("providerRating", providerRating);
        features.put("duration", booking.getDuration());
        features.put("totalPrice", booking.getTotalPrice() != null ? booking.getTotalPrice().doubleValue() : 0.0);

        LocalDateTime date = booking.getDate() != null ? booking.getDate() : booking.getCreatedAt();
        features.put("dayOfWeek", date != null ? date.getDayOfWeek().getValue() - 1 : 0);
        features.put("hour", date != null ? date.getHour() : 12);
        features.put("providerCompletedCount", providerCompleted);
        features.put("providerCancelledCount", providerCancelled);

        return features;
    }

    private Map<String, Object> buildProjectFeatures(Project project) {
        Map<String, Object> features = new LinkedHashMap<>();

        int teamSize = project.getMembers() != null ? project.getMembers().size() : 1;
        List<ProjectMilestone> milestones = milestoneRepository.findByProjectIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(project.getId());
        int milestoneCount = milestones.size();

        int blockedCount = 0;
        int completedCount = 0;
        double totalPlannedDuration = 0;
        double blockedDuration = 0;
        double completedDuration = 0;

        for (ProjectMilestone m : milestones) {
            if (m.getStatus() == ProjectMilestoneStatus.BLOCKED) blockedCount++;
            if (m.getStatus() == ProjectMilestoneStatus.COMPLETED) completedCount++;

            if (m.getPlannedStartDate() != null && m.getPlannedEndDate() != null) {
                long days = (m.getPlannedEndDate().getTime() - m.getPlannedStartDate().getTime()) / (1000 * 60 * 60 * 24);
                totalPlannedDuration += Math.max(days, 1);
                if (m.getStatus() == ProjectMilestoneStatus.COMPLETED) completedDuration += Math.max(days, 1);
                if (m.getStatus() == ProjectMilestoneStatus.BLOCKED) blockedDuration += Math.max(days, 1);
            }
        }

        double completedPct = milestoneCount > 0 ? (double) completedCount / milestoneCount : 0.0;
        double avgDuration = milestoneCount > 0 ? totalPlannedDuration / milestoneCount : 0.0;
        double dependencyDensity = 0.0;

        double totalPlannedDays = 0;
        if (project.getStartDate() != null && project.getEstimatedEndDate() != null) {
            totalPlannedDays = (project.getEstimatedEndDate().getTime() - project.getStartDate().getTime()) / (1000.0 * 60 * 60 * 24);
        }

        double slippage = 0.0;
        for (ProjectMilestone m : milestones) {
            if (m.getPlannedEndDate() != null && m.getStatus() != ProjectMilestoneStatus.COMPLETED
                    && m.getStatus() != ProjectMilestoneStatus.CANCELLED) {
                long plannedEndMs = m.getPlannedEndDate().getTime();
                long nowMs = System.currentTimeMillis();
                if (nowMs > plannedEndMs) {
                    slippage += (nowMs - plannedEndMs) / (1000.0 * 60 * 60 * 24);
                }
            }
        }

        features.put("teamSize", teamSize);
        features.put("milestoneCount", milestoneCount);
        features.put("budget", project.getBudget() != null ? project.getBudget().doubleValue() : 0.0);
        features.put("avgMilestoneDurationDays", avgDuration);
        features.put("blockedMilestoneCount", blockedCount);
        features.put("completedMilestonePct", completedPct);
        features.put("totalPlannedDays", totalPlannedDays);
        features.put("dependencyDensity", dependencyDensity);
        features.put("currentSlippageDays", slippage);

        return features;
    }

    private BookingPredictionResponse fallbackBookingPrediction(Booking booking) {
        BookingPredictionResponse resp = new BookingPredictionResponse();
        resp.setCompletionProbability(0.7);
        resp.setRiskLevel("MEDIUM");
        resp.setConfidence("LOW");
        resp.setKeyFactors(List.of("ML service unavailable — showing default estimate"));
        resp.setRecommendation("Proceed with standard booking flow.");
        return resp;
    }

    private ProjectDelayPredictionResponse fallbackProjectPrediction(Project project) {
        ProjectDelayPredictionResponse resp = new ProjectDelayPredictionResponse();
        resp.setOnTimeProbability(0.65);
        resp.setDelayRiskLevel("MEDIUM");
        resp.setEstimatedDelayDays(null);
        resp.setKeyFactors(List.of("ML service unavailable — showing default estimate"));
        resp.setRecommendation("Monitor project milestones closely.");
        return resp;
    }
}
