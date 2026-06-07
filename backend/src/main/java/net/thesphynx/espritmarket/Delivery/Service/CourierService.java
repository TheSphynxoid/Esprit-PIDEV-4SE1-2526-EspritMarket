package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Delivery.Dto.AdminCourierInfoResponse;
import net.thesphynx.espritmarket.Delivery.Dto.CourierInterviewDateRequest;
import net.thesphynx.espritmarket.Delivery.Dto.CourierInterviewDateResponse;
import net.thesphynx.espritmarket.Delivery.Dto.DeliveryCourierContactResponse;
import net.thesphynx.espritmarket.Delivery.Dto.CourierProfileResponse;
import net.thesphynx.espritmarket.Delivery.Dto.CourierProfileUpdateRequest;
import net.thesphynx.espritmarket.Delivery.Dto.CourierStatisticsResponse;
import net.thesphynx.espritmarket.Delivery.Dto.TopCourierResponse;
import net.thesphynx.espritmarket.Delivery.Entity.Courier;
import net.thesphynx.espritmarket.Delivery.Entity.CourierProfileStatus;
import net.thesphynx.espritmarket.Delivery.Entity.CourierStatus;
import net.thesphynx.espritmarket.Delivery.Entity.Vehicule;
import net.thesphynx.espritmarket.Delivery.Repository.ICourierRepository;
import net.thesphynx.espritmarket.Delivery.Repository.IVehiculeRepository;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class CourierService {
    private static final Logger log = LoggerFactory.getLogger(CourierService.class);
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final ICourierRepository courierRepository;
    private final IVehiculeRepository vehiculeRepository;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:no-reply@espritmarket.local}")
    private String interviewMailFrom;

    @Value("${courier.scheduler.pending-to-refused-threshold:PT5S}")
    private Duration pendingToRefusedThreshold;

    public CourierService(ICourierRepository courierRepository,
                          IVehiculeRepository vehiculeRepository,
                          UserRepository userRepository,
                          JavaMailSender javaMailSender) {
        this.courierRepository = courierRepository;
        this.vehiculeRepository = vehiculeRepository;
        this.userRepository = userRepository;
        this.javaMailSender = javaMailSender;
    }

    public List<AdminCourierInfoResponse> getCouriersForAdminTable() {
        return courierRepository.findAllBy().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryCourierContactResponse> getCourierContactByVehiculeId(Long vehiculeId) {
        return vehiculeRepository.findById(vehiculeId)
                .flatMap(vehicule -> {
                    if (vehicule.getUser() == null || vehicule.getUser().getId() == null) {
                        return Optional.empty();
                    }
                    return courierRepository.findByUserId(vehicule.getUser().getId());
                })
                .map(this::toDeliveryCourierContactResponse);
    }

    @Transactional(readOnly = true)
    public CourierStatisticsResponse getCourierStatistics(Long courierId) {
        return courierRepository.findCourierStatisticsByCourierId(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", courierId));
    }

    @Transactional(readOnly = true)
    public List<TopCourierResponse> getTopCouriersByDeliveredDeliveries(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return courierRepository.findTopCouriersByDeliveredDeliveries(PageRequest.of(0, safeLimit));
    }

    public void createCourierProfileIfNeeded(User user) {
        if (user == null || user.getRole() != Role.COURIER) {
            return;
        }

        courierRepository.findByUserId(user.getId()).orElseGet(() -> {
            Courier courier = Courier.builder().user(user).build();
            return courierRepository.save(courier);
        });
    }

    @Scheduled(cron = "${courier.scheduler.pending-to-refused-cron:*/5 * * * * *}")
    @Transactional
    public void refuseOldPendingCouriers() {
        LocalDateTime cutoff = LocalDateTime.now().minus(pendingToRefusedThreshold);
        List<Courier> pendingCouriers = courierRepository.findByStatus(CourierStatus.PENDING);
        List<Courier> expiredPendingCouriers = pendingCouriers.stream()
                .filter(courier -> isExpiredPendingCourier(courier, cutoff))
                .collect(Collectors.toList());

        if (expiredPendingCouriers.isEmpty()) {
            return;
        }

        for (Courier courier : expiredPendingCouriers) {
            courier.setStatus(CourierStatus.REFUSED);
            courierRepository.save(courier);
        }

        log.info("Updated {} pending couriers to refused", expiredPendingCouriers.size());
    }

    private boolean isExpiredPendingCourier(Courier courier, LocalDateTime cutoff) {
        // Old rows may not have createdAt yet. Consider them expired so they can be processed.
        return courier.getCreatedAt() == null || !courier.getCreatedAt().isAfter(cutoff);
    }

    public CourierProfileResponse getMyProfile(String email) {
        Courier courier = getCourierByEmail(email);
        return toResponse(courier);
    }

    public CourierProfileResponse uploadPermitImage(String email, MultipartFile file) throws IOException {
        Courier courier = getCourierByEmail(email);
        validatePermitImage(file);

        courier.setPermitFileName(file.getOriginalFilename());
        courier.setPermitContentType(file.getContentType());
        courier.setPermitImage(file.getBytes());

        Courier saved = courierRepository.save(courier);
        return toResponse(saved);
    }

    public CourierProfileResponse updateMyPhoneNumber(String email, CourierProfileUpdateRequest request) {
        Courier courier = getCourierByEmail(email);

        String phoneNumber = request != null ? request.getPhoneNumber() : null;
        if (phoneNumber != null) {
            phoneNumber = phoneNumber.trim();
            if (phoneNumber.isEmpty()) {
                phoneNumber = null;
            }
        }

        courier.setPhoneNumber(phoneNumber);
        Courier saved = courierRepository.save(courier);
        return toResponse(saved);
    }

    public CourierProfileResponse markProfileCompletedAfterFaceVerification(String email) {
        Courier courier = getCourierByEmail(email);
        courier.setProfileStatus(CourierProfileStatus.COMPLETED);
        Courier saved = courierRepository.save(courier);
        return toResponse(saved);
    }

    public Courier getMyCourierWithImage(String email) {
        return getCourierByEmail(email);
    }

    private Courier getCourierByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (user.getRole() != Role.COURIER) {
            throw new IllegalArgumentException("Cette action est reservee uniquement aux comptes courier");
        }

        return courierRepository.findByUserId(user.getId())
                .orElseGet(() -> courierRepository.save(Courier.builder().user(user).build()));
    }

    private void validatePermitImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image permit obligatoire");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image trop volumineuse (max 5 MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier doit etre une image");
        }
    }

    private CourierProfileResponse toResponse(Courier courier) {
        CourierProfileStatus profileStatus = courier.getProfileStatus();
        if (profileStatus == null) {
            profileStatus = CourierProfileStatus.INCOMPLETE;
        }

        return CourierProfileResponse.builder()
                .id(courier.getId())
                .userId(courier.getUser().getId())
                .phoneNumber(courier.getPhoneNumber())
                .permitFileName(courier.getPermitFileName())
                .permitContentType(courier.getPermitContentType())
                .permitImageUploaded(courier.getPermitImage() != null && courier.getPermitImage().length > 0)
                .interviewDate(courier.getInterviewDate())
                .status(courier.getStatus())
                .profileStatus(profileStatus)
                .build();
    }


    private AdminCourierInfoResponse toAdminResponse(Courier courier) {
        User user = courier.getUser();
        List<Vehicule> vehicules = vehiculeRepository.findByUserId(user.getId());

        String[] names = splitFullName(user.getName());
        List<AdminCourierInfoResponse.VehicleInfoDto> voitures = vehicules.stream()
                .filter(v -> v.getRegistrationnumbers() != null && !v.getRegistrationnumbers().isBlank())
                .map(v -> AdminCourierInfoResponse.VehicleInfoDto.builder()
                        .serie(v.getRegistrationnumbers())
                        .type(v.getType())
                        .build())
                .toList();

        return AdminCourierInfoResponse.builder()
                .courierId(courier.getId())
                .userId(user.getId())
                .nom(names[0])
                .prenom(names[1])
                .email(user.getEmail())
                .phoneNumber(courier.getPhoneNumber())
                .voitures(voitures)
                .permitImage(toPermitImageDataUri(courier))
                .interviewDate(courier.getInterviewDate())
                .status(courier.getStatus())
                .build();
    }

    private String toPermitImageDataUri(Courier courier) {                 //utilisé pour afficher dans frontend
        if (courier.getPermitImage() == null || courier.getPermitImage().length == 0) {
            return null;
        }

        String contentType = courier.getPermitContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        String base64 = Base64.getEncoder().encodeToString(courier.getPermitImage());
        return "data:" + contentType + ";base64," + base64;
    }

    public AdminCourierInfoResponse updateCourierStatus(Long courierId, CourierStatus newStatus) {
        Courier courier = getCourierById(courierId);

        courier.setStatus(newStatus);
        Courier saved = courierRepository.save(courier);
        return toAdminResponse(saved);
    }

    public CourierInterviewDateResponse createInterviewDate(Long courierId, CourierInterviewDateRequest request) {
        Courier courier = getCourierById(courierId);
        if (courier.getInterviewDate() != null) {
            throw new IllegalArgumentException("La date d'entretien existe deja pour ce livreur");
        }

        courier.setInterviewDate(validateInterviewDate(request.getInterviewDate()));
        Courier saved = courierRepository.save(courier);
        sendInterviewDateEmailIfRequested(saved, request);
        return toInterviewDateResponse(saved);
    }

    public CourierInterviewDateResponse getInterviewDate(Long courierId) {
        Courier courier = getCourierById(courierId);
        return toInterviewDateResponse(courier);
    }

    public CourierInterviewDateResponse updateInterviewDate(Long courierId, CourierInterviewDateRequest request) {
        Courier courier = getCourierById(courierId);
        courier.setInterviewDate(validateInterviewDate(request.getInterviewDate()));
        Courier saved = courierRepository.save(courier);
        sendInterviewDateEmailIfRequested(saved, request);
        return toInterviewDateResponse(saved);
    }

    public void deleteInterviewDate(Long courierId) {
        Courier courier = getCourierById(courierId);
        if (courier.getInterviewDate() == null) {
            throw new IllegalArgumentException("Aucune date d'entretien a supprimer pour ce livreur");
        }

        courier.setInterviewDate(null);
        courierRepository.save(courier);
    }

    private Courier getCourierById(Long courierId) {
        return courierRepository.findWithUserById(courierId)
                .orElseThrow(() -> new IllegalArgumentException("Courier non trouve avec l'ID: " + courierId));
    }

    private LocalDateTime validateInterviewDate(LocalDateTime interviewDate) {
        if (interviewDate == null) {
            throw new IllegalArgumentException("La date d'entretien est obligatoire");
        }

        return interviewDate;
    }

    private void sendInterviewDateEmailIfRequested(Courier courier, CourierInterviewDateRequest request) {
        if (request == null || !Boolean.TRUE.equals(request.getSendEmail())) {
            return;
        }

        User user = courier.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Impossible d'envoyer le mail: email utilisateur introuvable");
        }

        String customMessage = request.getNotificationMessage();
        String messageLine = (customMessage == null || customMessage.isBlank())
                ? "Here is your interview date."
                : customMessage.trim();

        String fullName = user.getName() == null || user.getName().isBlank() ? "Utilisateur" : user.getName();
        String dateText = courier.getInterviewDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE));

       SimpleMailMessage message = new SimpleMailMessage();
message.setFrom(interviewMailFrom);
message.setTo(user.getEmail());
message.setSubject("Esprit Market - Interview Invitation");

message.setText(
    "Dear " + fullName + ",\n\n" +
    "We are pleased to inform you that you have successfully passed the previous stage.\n\n" +
    messageLine + "\n\n" +
    "📅 Interview Details:\n" +
    "Date & Time: " + dateText + "\n\n" +
    "Please make sure to be available at the scheduled time.\n\n" +
    "Best regards,\n" +
    "Esprit Market Team"
);

javaMailSender.send(message);
}

    private CourierInterviewDateResponse toInterviewDateResponse(Courier courier) {
        return CourierInterviewDateResponse.builder()
                .courierId(courier.getId())
                .interviewDate(courier.getInterviewDate())
                .build();
    }

    private String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"", ""};
        }

        String normalized = fullName.trim().replaceAll("\\s+", " ");
        String[] parts = normalized.split(" ", 2);

        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }

        return new String[]{parts[0], parts[1]};
    }

    private DeliveryCourierContactResponse toDeliveryCourierContactResponse(Courier courier) {
        User user = courier.getUser();
        return DeliveryCourierContactResponse.builder()
                .courierId(courier.getId())
                .userId(user != null ? user.getId() : null)
                .name(user != null ? user.getName() : null)
                .email(user != null ? user.getEmail() : null)
                .phoneNumber(courier.getPhoneNumber())
                .build();
    }
}
