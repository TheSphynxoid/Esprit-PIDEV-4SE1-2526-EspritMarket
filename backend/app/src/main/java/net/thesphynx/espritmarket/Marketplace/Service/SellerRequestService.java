package net.thesphynx.espritmarket.Marketplace.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ConflictException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Marketplace.Dto.SellerRequestRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.SellerRequestResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.RequestStatus;
import net.thesphynx.espritmarket.Marketplace.Entity.SellerRequest;
import net.thesphynx.espritmarket.Marketplace.Entity.VerificationEntry;
import net.thesphynx.espritmarket.Marketplace.Mapper.SellerRequestMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.ISellerRequestRepository;

@Service
public class SellerRequestService {

    private static final Logger logger = LoggerFactory.getLogger(SellerRequestService.class);

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private final ISellerRequestRepository sellerRequestRepository;
    private final UserRepository userRepository;
    private final SellerRequestMapper sellerRequestMapper;
    private final net.thesphynx.espritmarket.Common.Service.EmailService emailService;
    private final String uploadDir;

    // Stockage temporaire des codes en mémoire (clé = email normalisé)
    private final Map<String, VerificationEntry> pendingVerifications = new ConcurrentHashMap<>();

    public SellerRequestService(ISellerRequestRepository sellerRequestRepository,
            UserRepository userRepository,
            SellerRequestMapper sellerRequestMapper,
            net.thesphynx.espritmarket.Common.Service.EmailService emailService,
            @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.sellerRequestRepository = sellerRequestRepository;
        this.userRepository = userRepository;
        this.sellerRequestMapper = sellerRequestMapper;
        this.emailService = emailService;
        this.uploadDir = uploadDir;
    }

    // ─── Envoie le code par email ─────────────────────────────────────
    // ✅ Ne retourne PLUS le code (sécurité) — le code part uniquement par email
    public Map<String, String> sendVerificationCode(String email) {
        logger.info("📧 [Service] sendVerificationCode called for: {}", maskEmail(email));

        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email cannot be empty");
        }

        email = email.trim().toLowerCase();

        if (!email.endsWith("@esprit.tn")) {
            throw new BadRequestException("Email must end with @esprit.tn");
        }

        if (!email.matches("^[a-z0-9._%+\\-]+@esprit\\.tn$")) {
            throw new BadRequestException("Invalid email format");
        }

        // ✅ Génère un code à 5 chiffres (00000 à 99999)
        String code = String.format("%05d", SECURE_RANDOM.nextInt(100000));
        logger.info("🔑 [Service] Code generated for: {}", maskEmail(email));

        // Stocke le code en mémoire avec expiration 10 min
        pendingVerifications.put(email, new VerificationEntry(code, email));

        // Envoie le code par email — ne le retourne PAS dans la réponse HTTP
        try {
            emailService.sendVerificationCode(email, code);
            logger.info("✅ [Service] Verification email sent to: {}", maskEmail(email));
        } catch (Exception ex) {
            // Supprime le code si l'envoi échoue
            pendingVerifications.remove(email);
            logger.error("❌ [Service] Failed to send email to: {}", maskEmail(email), ex);
            throw new BadRequestException("Failed to send verification code. Please try again later.");
        }

        return Map.of("message", "Verification code sent to " + maskEmail(email));
    }

    // ─── Vérifie le code entré par l'étudiant ────────────────────────
    @Transactional
    public Map<String, Object> verifyCode(String email, String code, Long callerId) {
        if (email == null || code == null) {
            throw new BadRequestException("Email and code are required");
        }

        String normalizedEmail = email.toLowerCase().trim();
        VerificationEntry entry = pendingVerifications.get(normalizedEmail);

        // Code inexistant
        if (entry == null) {
            throw new BadRequestException("No verification code found for this email. Please request a new code.");
        }

        // Code expiré
        if (entry.isExpired()) {
            pendingVerifications.remove(normalizedEmail);
            throw new BadRequestException("Verification code has expired. Please request a new code.");
        }

        // Code incorrect
        if (!entry.getCode().equals(code.trim())) {
            throw new BadRequestException("Incorrect verification code. Please try again.");
        }

        // ✅ Code correct → on supprime et on continue
        pendingVerifications.remove(normalizedEmail);
        logger.info("✅ [Service] Code verified for: {}", maskEmail(email));

        // Securite (A5a): lie la demande au compte du principal, JAMAIS au userId
        // du chemin (spoofable). callerId est resolu par le controller depuis
        // Authentication.getName() et ne peut donc designer que l'appelant. Sans
        // principal (impossible via HTTP, endpoint authentifie), on refuse : plus
        // de demande approuvee orpheline.
        if (callerId == null || callerId <= 0) {
            throw new BadRequestException("Authentication required to verify the code");
        }
        User user = userRepository.findById(callerId)
                .orElseThrow(() -> new BadRequestException(
                        "Authenticated account no longer exists"));

        // Le code a ete envoye sur une boite @esprit.tn : s'il ne s'agit pas de
        // celle du compte connecte, on refuse (empeche de rattacher le code
        // verifie d'un tiers a son propre compte). Le frontend envoie toujours
        // l'email du compte (champ pre-rempli et en lecture seule).
        if (!normalizedEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException(
                    "Verification email does not match the logged-in account");
        }

        // Crée la demande avec statut APPROUVE automatiquement
        SellerRequest sellerRequest = new SellerRequest();
        sellerRequest.setEmail(normalizedEmail);
        sellerRequest.setStatut(RequestStatus.APPROUVE);
        sellerRequest.setDateDemande(new Date());
        sellerRequest.setDateValidation(new Date());
        sellerRequest.setCarteEtudiantUrl("verified-by-email");

        String fullName = user.getName() != null ? user.getName() : "";
        String[] parts = fullName.split(" ", 2);
        sellerRequest.setPrenom(parts.length > 0 ? parts[0] : "");
        sellerRequest.setNom(parts.length > 1 ? parts[1] : "");
        sellerRequest.setUser(user);
        sellerRequest.setNumeroEtudiant("EMAIL-VERIFIED");

        sellerRequestRepository.save(sellerRequest);

        return Map.of(
            "success", true,
            "message", "Email verified successfully. You can now create your store.",
            "redirectTo", "/create-store-step-2"
        );
    }

    @Transactional
    public Map<String, Object> activateSeller(Long callerId) {
        // Securite (A2): l'identite vient du principal, pas du chemin. Le userId
        // fourni par l'UI est ignore : un utilisateur authentifie ne peut plus
        // promouvoir un compte arbitraire. Le chemin /activate-seller/{userId}
        // reste accepte pour compatibilite frontend.
        if (callerId == null || callerId <= 0) {
            throw new BadRequestException("A valid userId is required");
        }

        User user = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + callerId));

        // Une demande APPROUVE prouve l'eligibilite vendeur (email @esprit.tn
        // verifie par code ou validation admin). Sans elle, pas de passage SELLER.
        // Propriete etablie par user_id, ou a defaut par l'email du compte sur la
        // demande (lignes historiques sans relation user).
        boolean approved = sellerRequestRepository
                .existsByUserIdAndStatut(callerId, RequestStatus.APPROUVE)
                || sellerRequestRepository
                        .existsByEmailIgnoreCaseAndStatut(user.getEmail(), RequestStatus.APPROUVE);
        if (!approved) {
            throw new ConflictException(
                    "No approved seller request found for this account. Complete email verification first.");
        }

        if (user.getRole() != Role.SELLER) {
            user.setRole(Role.SELLER);
            userRepository.save(user);
            logger.info("✅ [Service] Role updated to SELLER for principal-derived userId: {}", callerId);
        }

        return Map.of(
            "success", true,
            "message", "Role updated to SELLER",
            "role", user.getRole().name()
        );
    }

    // ─── Méthodes existantes ──────────────────────────────────────────

    public List<SellerRequestResponse> getAllRequests() {
        List<SellerRequest> requests = sellerRequestRepository.findAll();
        logger.info("Fetched {} seller requests", requests.size());
        return requests.stream()
                .map(sellerRequestMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SellerRequestResponse createRequest(SellerRequestRequest request, Long userId) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (userId != null && userId < 0) {
            throw new BadRequestException("userId must be 0 (anonymous) or a valid positive id");
        }

        normalize(request);

        User user = null;
        if (userId != null && userId != 0) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        }

        if (sellerRequestRepository.existsByEmailIgnoreCaseAndStatut(request.getEmail(), RequestStatus.EN_ATTENTE)) {
            throw new ConflictException("A pending seller request already exists for this email");
        }
        if (user != null && sellerRequestRepository.existsByUserIdAndStatut(user.getId(), RequestStatus.EN_ATTENTE)) {
            throw new ConflictException("This user already has a pending seller request");
        }

        SellerRequest sellerRequest = sellerRequestMapper.toEntity(request);
        sellerRequest.setUser(user);
        sellerRequest.setStatut(RequestStatus.EN_ATTENTE);
        sellerRequest.setDateDemande(new Date());

        try {
            SellerRequest saved = sellerRequestRepository.save(sellerRequest);
            Long logUserId = (saved.getUser() != null && saved.getUser().getId() != null)
                    ? saved.getUser().getId() : 0L;
            logger.info("Seller request created: requestId={}, userId={}, email={}",
                    saved.getId(), logUserId, maskEmail(saved.getEmail()));
            return sellerRequestMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            logger.warn("Database constraint violation on seller request save", ex);
            throw new ConflictException("Unable to save seller request due to data constraints.");
        }
    }

    @Transactional
    public SellerRequestResponse createRequestWithCardFile(SellerRequestRequest request, Long userId,
            MultipartFile cardFile) {
        if (cardFile == null || cardFile.isEmpty()) {
            throw new BadRequestException("Student card file is required");
        }
        String publicUrl = storeStudentCard(cardFile);
        request.setCarteEtudiantUrl(publicUrl);
        return createRequest(request, userId);
    }

    @Transactional
    public SellerRequestResponse approveRequest(Long requestId, String adminEmail) {
        SellerRequest sellerRequest = sellerRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        User validatedBy = getUserByEmailOrThrow(adminEmail);

        if (sellerRequest.getStatut() != RequestStatus.EN_ATTENTE) {
            throw new ConflictException("Only pending requests can be approved");
        }

        sellerRequest.setStatut(RequestStatus.APPROUVE);
        sellerRequest.setValidatedBy(validatedBy);
        sellerRequest.setDateValidation(new Date());

        // Securite (Track A): plus de creation de compte fantome avec mot de passe
        // aléatoire a l'approbation. Le demandeur est toujours un utilisateur reel
        // (la creation d'une demande est bloquee sans session). Si aucune relation
        // n'existe (donnees historiques), on retombe sur les noms de la demande,
        // comme dans refuseRequest.
        User user = sellerRequest.getUser();
        String recipientName = (user != null && user.getName() != null)
                ? user.getName()
                : (sellerRequest.getPrenom() + " " + sellerRequest.getNom());

        try {
            emailService.sendApprovalEmail(sellerRequest.getEmail(),
                    recipientName, user != null ? user.getId() : 0L);
        } catch (RuntimeException ex) {
            logger.warn("Approval email failed for requestId={}", requestId, ex);
        }

        return sellerRequestMapper.toResponse(sellerRequestRepository.save(sellerRequest));
    }

    @Transactional
    public SellerRequestResponse refuseRequest(Long requestId, String adminEmail) {
        SellerRequest sellerRequest = sellerRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        User validatedBy = getUserByEmailOrThrow(adminEmail);

        if (sellerRequest.getStatut() != RequestStatus.EN_ATTENTE) {
            throw new ConflictException("Only pending requests can be refused");
        }

        sellerRequest.setStatut(RequestStatus.REFUSE);
        sellerRequest.setValidatedBy(validatedBy);
        sellerRequest.setDateValidation(new Date());

        SellerRequest saved = sellerRequestRepository.save(sellerRequest);

        User user = saved.getUser();
        String name = (user != null && user.getName() != null)
                ? user.getName()
                : (saved.getPrenom() + " " + saved.getNom());
        try {
            emailService.sendRejectionEmail(saved.getEmail(), name);
        } catch (RuntimeException ex) {
            logger.warn("Rejection email failed for requestId={}", requestId, ex);
        }

        return sellerRequestMapper.toResponse(saved);
    }

    public SellerRequestResponse getRequestByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("A valid userId is required");
        }
        return sellerRequestRepository.findTopByUserIdOrderByDateDemandeDesc(userId)
                .map(sellerRequestMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found for user ID: " + userId));
    }

    /**
     * Resout l'id utilisateur depuis l'email du principal (Authentication.getName()),
     * meme contrat que la resolution par email utilisee dans OrderService : le
     * compte doit exister. Expose ici pour eviter une dependance croisee.
     */
    public Long resolveUserIdFromAuthentication(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + maskEmail(email)));
    }

    /**
     * Resout un utilisateur a partir de son email. L'email provient toujours du
     * principal (Authentication.getName()) : le record d'approbation
     * (sellerRequest.validatedBy) ne peut plus etre forge via un adminId arbitraire.
     */
    private User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with email: " + maskEmail(email)));
    }

    // ─── Helpers privés ───────────────────────────────────────────────

    private void normalize(SellerRequestRequest request) {
        request.setNumeroEtudiant(trimToNull(request.getNumeroEtudiant()));
        request.setPrenom(trimToNull(request.getPrenom()));
        request.setNom(trimToNull(request.getNom()));
        request.setCarteEtudiantUrl(trimToNull(request.getCarteEtudiantUrl()));
        String normalizedEmail = trimToNull(request.getEmail());
        if (normalizedEmail != null) {
            request.setEmail(normalizedEmail.toLowerCase(Locale.ROOT));
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String storeStudentCard(MultipartFile file) {
        String original = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "card";
        String extension = "";
        int dotIndex = original.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = original.substring(dotIndex);
        }
        String generatedName = UUID.randomUUID() + extension;
        Path studentCardsDir = Path.of(uploadDir).resolve("student-cards");
        Path targetPath = studentCardsDir.resolve(generatedName).normalize().toAbsolutePath();
        try {
            Files.createDirectories(studentCardsDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            logger.error("Failed to store student card file", ex);
            throw new BadRequestException("Unable to store student card file");
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/student-cards/")
                .path(generatedName)
                .toUriString();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@", 2);
        if (parts[0].length() <= 2) return "***@" + parts[1];
        return parts[0].substring(0, 2) + "***@" + parts[1];
    }
}