package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Common.Exception.ConflictException;
import net.thesphynx.espritmarket.Common.Exception.ForbiddenException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Marketplace.Dto.StoreRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.StoreResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Category;
import net.thesphynx.espritmarket.Marketplace.Entity.Store;
import net.thesphynx.espritmarket.Marketplace.Mapper.StoreMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.ICategoryRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.IStoreRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.ISellerRequestRepository;
import net.thesphynx.espritmarket.Marketplace.Entity.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class StoreService {
    private static final Logger logger = LoggerFactory.getLogger(StoreService.class);
    private static final int MAX_STORES_PER_OWNER = 3;

    private final IStoreRepository storeRepository;
    private final ICategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StoreMapper storeMapper;
    private final ImageStorageService imageStorageService;
    private final ISellerRequestRepository sellerRequestRepository;

    public StoreService(IStoreRepository storeRepository,
            ICategoryRepository categoryRepository,
            UserRepository userRepository,
            StoreMapper storeMapper,
            ImageStorageService imageStorageService,
            ISellerRequestRepository sellerRequestRepository) {
        this.storeRepository = storeRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.storeMapper = storeMapper;
        this.imageStorageService = imageStorageService;
        this.sellerRequestRepository = sellerRequestRepository;
    }

    @Transactional
    public StoreResponse createStore(StoreRequest request, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Valider que l'utilisateur a une demande de seller APPROUVE
        validateSellerApprovalStatus(ownerId);

        long existingStores = storeRepository.countByOwnerId(ownerId);
        if (existingStores >= MAX_STORES_PER_OWNER) {
            throw new ConflictException("Limite atteinte: maximum 3 boutiques par seller.");
        }

        Store store = storeMapper.toEntity(request);
        store.setOwner(owner);
        store.setRating(0.0);

        final Store savedStore = storeRepository.save(store);

        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            List<Category> categories = new ArrayList<>();
            for (String catName : request.getCategories()) {
                Category category = new Category();
                category.setName(catName);
                category.setStore(savedStore);
                categories.add(categoryRepository.save(category));
            }
            savedStore.setCategories(categories);
        }

        return storeMapper.toResponse(savedStore);
    }

    @Transactional
    public StoreResponse createMyStore(StoreRequest request, String userEmail) {
        Long ownerId = resolveUserIdByEmail(userEmail);
        logger.info("Creating personal store for userId={}", ownerId);
        return createStore(request, ownerId);
    }

    @Transactional
    public StoreResponse createWithLogo(String name, String description, String address, String phone,
                                         List<String> categories, MultipartFile logo, String userEmail) {
        if (name == null || name.strip().length() < 3) {
            throw new net.thesphynx.espritmarket.Common.Exception.BadRequestException("Store name must have at least 3 characters");
        }
        if (description == null || description.strip().length() < 10) {
            throw new net.thesphynx.espritmarket.Common.Exception.BadRequestException("Description must contain at least 10 characters");
        }
        if (address == null || address.isBlank()) {
            throw new net.thesphynx.espritmarket.Common.Exception.BadRequestException("Store address is required");
        }
        if (phone == null || !phone.matches("^\\+?[0-9\\s]{8,16}$")) {
            throw new net.thesphynx.espritmarket.Common.Exception.BadRequestException("Use only digits/spaces, optional +, length 8-16");
        }
        if (logo == null || logo.isEmpty()) {
            throw new net.thesphynx.espritmarket.Common.Exception.BadRequestException("Store logo is required");
        }

        String logoUrl = imageStorageService.store(logo);
        StoreRequest request2 = new StoreRequest();
        request2.setName(name.strip());
        request2.setDescription(description.strip());
        request2.setAddress(address.strip());
        request2.setPhone(phone.strip());
        request2.setLogoUrl(logoUrl);
        request2.setCategories(categories);

        return createMyStore(request2, userEmail);
    }

    @Transactional(readOnly = true)
    public StoreResponse getMyStore(String userEmail) {
        Long ownerId = resolveUserIdByEmail(userEmail);
        // FIX — findByOwnerId crash si 2 stores existent → utilise findFirst
        Store store = storeRepository.findFirstByOwnerIdOrderByIdAsc(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for current user"));
        return storeMapper.toResponse(store);
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getMyStores(String userEmail) {
        Long ownerId = resolveUserIdByEmail(userEmail);
        return storeRepository.findAllByOwnerIdOrderByIdAsc(ownerId)
                .stream()
                .map(storeMapper::toResponse)
                .toList();
    }

    @Transactional
    public StoreResponse getOrCreateMyStore(String userEmail) {
        Long ownerId = resolveUserIdByEmail(userEmail);
        // FIX — findByOwnerId crash si 2 stores existent → utilise findFirst
        return storeRepository.findFirstByOwnerIdOrderByIdAsc(ownerId)
                .map(storeMapper::toResponse)
                .orElseGet(() -> {
                    User owner = userRepository.findById(ownerId).orElseThrow();
                    StoreRequest request = new StoreRequest();
                    request.setName("Boutique de " + owner.getName());
                    request.setDescription("Ceci est la boutique officielle de " + owner.getName() + " sur Esprit Market.");
                    request.setAddress("Adresse à renseigner");
                    request.setPhone("00000000");
                    request.setLogoUrl("https://ui-avatars.com/api/?name=" + owner.getName());
                    return createStore(request, ownerId);
                });
    }

    @Transactional(readOnly = true)
    public StoreResponse getById(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id=" + storeId));
        return storeMapper.toResponse(store);
    }

    @Transactional(readOnly = true)
    public Store getMyStoreEntity(String userEmail) {
        Long ownerId = resolveUserIdByEmail(userEmail);
        // FIX — findByOwnerId crash si 2 stores existent → utilise findFirst
        return storeRepository.findFirstByOwnerIdOrderByIdAsc(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for current user"));
    }

    @Transactional(readOnly = true)
    public Store getOwnedStoreEntity(String userEmail, Long storeId) {
        Long ownerId = resolveUserIdByEmail(userEmail);

        if (storeId != null && storeId > 0) {
            return storeRepository.findByIdAndOwnerId(storeId, ownerId)
                    .orElseThrow(() -> new ForbiddenException("You can only access your own store"));
        }

        return storeRepository.findFirstByOwnerIdOrderByIdAsc(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for current user"));
    }

    private Long resolveUserIdByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    /**
     * Valide que l'utilisateur a une demande de seller APPROUVE
     * Cela s'assure que la création de store n'est possible que si:
     * 1. L'utilisateur a complété Step 1 (email verification)
     * 2. Sa demande a été approuvée par l'admin
     */
    private void validateSellerApprovalStatus(Long userId) {
        var latestRequest = sellerRequestRepository.findTopByUserIdOrderByDateDemandeDesc(userId);
        
        if (latestRequest.isEmpty()) {
            throw new ForbiddenException(
                "You must complete the email verification process (Step 1) before creating a store");
        }
        
        var request = latestRequest.get();
        if (request.getStatut() != RequestStatus.APPROUVE) {
            throw new ForbiddenException(
                "Your seller request must be approved before you can create a store. Current status: " + request.getStatut());
        }
        
        logger.info("Seller approval validated for userId={}, status={}", userId, request.getStatut());
    }

    public List<Store> getAllStores() {
        return storeRepository.findAll();
    }

    @Transactional
    public void deleteStore(String userEmail, Long storeId) {
        // Ensure the authenticated user owns the store (will throw ForbiddenException otherwise)
        Store owned = getOwnedStoreEntity(userEmail, storeId);
        storeRepository.delete(owned);
    }
}