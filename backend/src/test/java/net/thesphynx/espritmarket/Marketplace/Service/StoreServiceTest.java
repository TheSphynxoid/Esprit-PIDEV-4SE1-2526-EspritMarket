package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.ConflictException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Marketplace.Dto.StoreRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.StoreResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Store;
import net.thesphynx.espritmarket.Marketplace.Mapper.StoreMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.ICategoryRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.ISellerRequestRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.IStoreRepository;
import net.thesphynx.espritmarket.Marketplace.Entity.RequestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private IStoreRepository storeRepository;

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreMapper storeMapper;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private ISellerRequestRepository sellerRequestRepository;

    @InjectMocks
    private StoreService service;

    @Test
    void getAllStores_shouldReturnAllStores() {
        var e1 = new Store();
        var e2 = new Store();

        when(storeRepository.findAll()).thenReturn(List.of(e1, e2));

        var result = service.getAllStores();

        assertEquals(2, result.size());
        verify(storeRepository).findAll();
    }

    @Test
    void getById_whenFound_shouldReturnResponse() {
        var id = 1L;
        var entity = new Store();
        var response = new StoreResponse();
        when(storeRepository.findById(id)).thenReturn(Optional.of(entity));
        when(storeMapper.toResponse(entity)).thenReturn(response);

        var result = service.getById(id);

        assertEquals(response, result);
        verify(storeRepository).findById(id);
    }

    @Test
    void getById_whenMissing_shouldThrow() {
        var id = 404L;
        when(storeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
    }

    @Test
    void createStore_shouldPersistAndReturnResponse() {
        var ownerId = 1L;
        var owner = new User();
        owner.setId(ownerId);
        var request = new StoreRequest();
        request.setCategories(Collections.emptyList());
        var entity = new Store();
        var response = new StoreResponse();
        var sellerRequest = new net.thesphynx.espritmarket.Marketplace.Entity.SellerRequest();
        sellerRequest.setStatut(RequestStatus.APPROUVE);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(sellerRequestRepository.findTopByUserIdOrderByDateDemandeDesc(ownerId)).thenReturn(Optional.of(sellerRequest));
        when(storeRepository.countByOwnerId(ownerId)).thenReturn(0L);
        when(storeMapper.toEntity(request)).thenReturn(entity);
        when(storeRepository.save(entity)).thenReturn(entity);
        when(storeMapper.toResponse(entity)).thenReturn(response);

        var result = service.createStore(request, ownerId);

        assertEquals(response, result);
        verify(storeMapper).toEntity(request);
        verify(storeRepository).save(entity);
    }

    @Test
    void createStore_whenOwnerNotFound_shouldThrow() {
        var ownerId = 99L;
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createStore(new StoreRequest(), ownerId));
    }

    @Test
    void createStore_whenOwnerAlreadyHasStore_shouldThrow() {
        var ownerId = 1L;
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(new User()));
        var sellerRequest = new net.thesphynx.espritmarket.Marketplace.Entity.SellerRequest();
        sellerRequest.setStatut(RequestStatus.APPROUVE);
        when(sellerRequestRepository.findTopByUserIdOrderByDateDemandeDesc(ownerId)).thenReturn(Optional.of(sellerRequest));
        when(storeRepository.countByOwnerId(ownerId)).thenReturn(3L);

        assertThrows(ConflictException.class,
                () -> service.createStore(new StoreRequest(), ownerId));
    }

    @Test
    void getMyStore_whenFound_shouldReturnResponse() {
        var email = "user@test.com";
        var user = new User();
        user.setId(1L);
        var entity = new Store();
        var response = new StoreResponse();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(storeRepository.findFirstByOwnerIdOrderByIdAsc(1L)).thenReturn(Optional.of(entity));
        when(storeMapper.toResponse(entity)).thenReturn(response);

        var result = service.getMyStore(email);

        assertEquals(response, result);
    }

    @Test
    void getMyStore_whenMissing_shouldThrow() {
        var email = "user@test.com";
        var user = new User();
        user.setId(1L);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(storeRepository.findFirstByOwnerIdOrderByIdAsc(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getMyStore(email));
    }

    @Test
    void getOrCreateMyStore_whenStoreExists_shouldReturnExisting() {
        var email = "user@test.com";
        var user = new User();
        user.setId(1L);
        var entity = new Store();
        var response = new StoreResponse();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(storeRepository.findFirstByOwnerIdOrderByIdAsc(1L)).thenReturn(Optional.of(entity));
        when(storeMapper.toResponse(entity)).thenReturn(response);

        var result = service.getOrCreateMyStore(email);

        assertEquals(response, result);
    }

    @Test
    void getOrCreateMyStore_whenStoreMissing_shouldCreate() {
        var email = "user@test.com";
        var user = new User();
        user.setId(1L);
        user.setName("TestUser");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(storeRepository.findFirstByOwnerIdOrderByIdAsc(1L)).thenReturn(Optional.empty());
        var sellerRequest = new net.thesphynx.espritmarket.Marketplace.Entity.SellerRequest();
        sellerRequest.setStatut(RequestStatus.APPROUVE);
        when(sellerRequestRepository.findTopByUserIdOrderByDateDemandeDesc(1L)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storeRepository.countByOwnerId(1L)).thenReturn(0L);
        when(storeMapper.toEntity(any(StoreRequest.class))).thenReturn(new Store());
        when(storeRepository.save(any(Store.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storeMapper.toResponse(any(Store.class))).thenReturn(new StoreResponse());

        var result = service.getOrCreateMyStore(email);

        assertNotNull(result);
        verify(storeRepository).save(any(Store.class));
    }

    @Test
    void getMyStoreEntity_shouldReturnStoreEntity() {
        var email = "user@test.com";
        var user = new User();
        user.setId(1L);
        var entity = new Store();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(storeRepository.findFirstByOwnerIdOrderByIdAsc(1L)).thenReturn(Optional.of(entity));

        var result = service.getMyStoreEntity(email);

        assertEquals(entity, result);
    }

    @Test
    void createMyStore_shouldResolveEmailAndCreate() {
        var email = "user@test.com";
        var user = new User();
        user.setId(1L);
        var request = new StoreRequest();
        request.setCategories(Collections.emptyList());
        var entity = new Store();
        var response = new StoreResponse();
        var sellerRequest = new net.thesphynx.espritmarket.Marketplace.Entity.SellerRequest();
        sellerRequest.setStatut(RequestStatus.APPROUVE);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(sellerRequestRepository.findTopByUserIdOrderByDateDemandeDesc(1L)).thenReturn(Optional.of(sellerRequest));
        when(storeRepository.countByOwnerId(1L)).thenReturn(0L);
        when(storeMapper.toEntity(request)).thenReturn(entity);
        when(storeRepository.save(entity)).thenReturn(entity);
        when(storeMapper.toResponse(entity)).thenReturn(response);

        var result = service.createMyStore(request, email);

        assertEquals(response, result);
        verify(userRepository).findByEmail(email);
    }
}
