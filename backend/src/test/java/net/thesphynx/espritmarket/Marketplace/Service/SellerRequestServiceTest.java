package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ConflictException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Common.Service.EmailService;
import net.thesphynx.espritmarket.Marketplace.Dto.SellerRequestRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.SellerRequestResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.RequestStatus;
import net.thesphynx.espritmarket.Marketplace.Entity.SellerRequest;
import net.thesphynx.espritmarket.Marketplace.Mapper.SellerRequestMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.ISellerRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerRequestServiceTest {

    @Mock
    private ISellerRequestRepository sellerRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SellerRequestMapper sellerRequestMapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private SellerRequestService service;

    private SellerRequestRequest buildRequest() {
        var req = new SellerRequestRequest();
        req.setNumeroEtudiant("12345");
        req.setPrenom("Jean");
        req.setNom("Dupont");
        req.setEmail("jean.dupont@esprit.tn");
        req.setCarteEtudiantUrl("http://example.com/card.jpg");
        return req;
    }

    @Test
    void getAllRequests_shouldReturnMappedList() {
        var e1 = new SellerRequest();
        var e2 = new SellerRequest();
        var r1 = new SellerRequestResponse();
        var r2 = new SellerRequestResponse();

        when(sellerRequestRepository.findAll()).thenReturn(List.of(e1, e2));
        when(sellerRequestMapper.toResponse(e1)).thenReturn(r1);
        when(sellerRequestMapper.toResponse(e2)).thenReturn(r2);

        var result = service.getAllRequests();

        assertEquals(2, result.size());
        verify(sellerRequestRepository).findAll();
    }

    @Test
    void createRequest_shouldPersistAndReturn() {
        var userId = 1L;
        var user = new User();
        user.setId(userId);
        var request = buildRequest();
        var entity = new SellerRequest();
        var response = new SellerRequestResponse();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sellerRequestRepository.existsByEmailIgnoreCaseAndStatut(request.getEmail(), RequestStatus.EN_ATTENTE)).thenReturn(false);
        when(sellerRequestRepository.existsByUserIdAndStatut(userId, RequestStatus.EN_ATTENTE)).thenReturn(false);
        when(sellerRequestMapper.toEntity(request)).thenReturn(entity);
        when(sellerRequestRepository.save(entity)).thenReturn(entity);
        when(sellerRequestMapper.toResponse(entity)).thenReturn(response);

        var result = service.createRequest(request, userId);

        assertEquals(response, result);
        verify(sellerRequestRepository).save(entity);
    }

    @Test
    void createRequest_withNullBody_shouldThrow() {
        assertThrows(BadRequestException.class, () -> service.createRequest(null, 1L));
    }

    @Test
    void createRequest_withNegativeUserId_shouldThrow() {
        var request = buildRequest();
        assertThrows(BadRequestException.class, () -> service.createRequest(request, -1L));
    }

    @Test
    void createRequest_whenUserNotFound_shouldThrow() {
        var userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createRequest(buildRequest(), userId));
    }

    @Test
    void createRequest_whenPendingEmailExists_shouldThrow() {
        var userId = 1L;
        var user = new User();
        user.setId(userId);
        var request = buildRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sellerRequestRepository.existsByEmailIgnoreCaseAndStatut(request.getEmail(), RequestStatus.EN_ATTENTE)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.createRequest(request, userId));
    }

    @Test
    void createRequest_whenUserAlreadyHasPending_shouldThrow() {
        var userId = 1L;
        var user = new User();
        user.setId(userId);
        var request = buildRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sellerRequestRepository.existsByEmailIgnoreCaseAndStatut(any(), any())).thenReturn(false);
        when(sellerRequestRepository.existsByUserIdAndStatut(userId, RequestStatus.EN_ATTENTE)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.createRequest(request, userId));
    }

    @Test
    void createRequest_anonymousWithUserIdZero_shouldSucceed() {
        var request = buildRequest();
        var entity = new SellerRequest();
        var response = new SellerRequestResponse();

        when(sellerRequestRepository.existsByEmailIgnoreCaseAndStatut(any(), any())).thenReturn(false);
        when(sellerRequestMapper.toEntity(request)).thenReturn(entity);
        when(sellerRequestRepository.save(entity)).thenReturn(entity);
        when(sellerRequestMapper.toResponse(entity)).thenReturn(response);

        var result = service.createRequest(request, 0L);

        assertEquals(response, result);
    }

    @Test
    void approveRequest_shouldSetStatusAndReturn() {
        var requestId = 1L;
        var adminId = 10L;
        var user = new User();
        user.setId(2L);
        user.setEmail("test@esprit.tn");
        user.setName("Test User");
        user.setRole(Role.USER);
        var admin = new User();
        admin.setId(adminId);
        var sellerRequest = new SellerRequest();
        sellerRequest.setStatut(RequestStatus.EN_ATTENTE);
        sellerRequest.setUser(user);
        sellerRequest.setEmail("test@esprit.tn");
        sellerRequest.setPrenom("Test");
        sellerRequest.setNom("User");

        var response = new SellerRequestResponse();

        when(sellerRequestRepository.findById(requestId)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(sellerRequestRepository.save(sellerRequest)).thenReturn(sellerRequest);
        when(sellerRequestMapper.toResponse(sellerRequest)).thenReturn(response);

        var result = service.approveRequest(requestId, adminId);

        assertEquals(response, result);
        assertEquals(RequestStatus.APPROUVE, sellerRequest.getStatut());
        verify(emailService).sendApprovalEmail(any(), any(), any());
    }

    @Test
    void approveRequest_whenNotPending_shouldThrow() {
        var requestId = 1L;
        var adminId = 10L;
        var admin = new User();
        admin.setId(adminId);
        var sellerRequest = new SellerRequest();
        sellerRequest.setStatut(RequestStatus.APPROUVE);
        sellerRequest.setUser(new User());

        when(sellerRequestRepository.findById(requestId)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThrows(ConflictException.class, () -> service.approveRequest(requestId, adminId));
    }

    @Test
    void approveRequest_whenRequestNotFound_shouldThrow() {
        when(sellerRequestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.approveRequest(99L, 1L));
    }

    @Test
    void refuseRequest_shouldSetStatusAndReturn() {
        var requestId = 1L;
        var adminId = 10L;
        var user = new User();
        user.setId(2L);
        user.setName("Test User");
        var admin = new User();
        admin.setId(adminId);
        var sellerRequest = new SellerRequest();
        sellerRequest.setStatut(RequestStatus.EN_ATTENTE);
        sellerRequest.setUser(user);
        sellerRequest.setEmail("test@esprit.tn");
        sellerRequest.setPrenom("Test");
        sellerRequest.setNom("User");

        var response = new SellerRequestResponse();

        when(sellerRequestRepository.findById(requestId)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(sellerRequestRepository.save(sellerRequest)).thenReturn(sellerRequest);
        when(sellerRequestMapper.toResponse(sellerRequest)).thenReturn(response);

        var result = service.refuseRequest(requestId, adminId);

        assertEquals(response, result);
        assertEquals(RequestStatus.REFUSE, sellerRequest.getStatut());
        verify(emailService).sendRejectionEmail(any(), any());
    }

    @Test
    void refuseRequest_whenNotPending_shouldThrow() {
        var requestId = 1L;
        var adminId = 10L;
        var admin = new User();
        admin.setId(adminId);
        var sellerRequest = new SellerRequest();
        sellerRequest.setStatut(RequestStatus.REFUSE);
        sellerRequest.setUser(new User());

        when(sellerRequestRepository.findById(requestId)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThrows(ConflictException.class, () -> service.refuseRequest(requestId, adminId));
    }

    @Test
    void getRequestByUserId_whenFound_shouldReturn() {
        var userId = 1L;
        var entity = new SellerRequest();
        var response = new SellerRequestResponse();

        when(sellerRequestRepository.findTopByUserIdOrderByDateDemandeDesc(userId)).thenReturn(Optional.of(entity));
        when(sellerRequestMapper.toResponse(entity)).thenReturn(response);

        var result = service.getRequestByUserId(userId);

        assertEquals(response, result);
    }

    @Test
    void getRequestByUserId_whenMissing_shouldThrow() {
        when(sellerRequestRepository.findTopByUserIdOrderByDateDemandeDesc(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getRequestByUserId(99L));
    }

    @Test
    void getRequestByUserId_withNullId_shouldThrow() {
        assertThrows(BadRequestException.class, () -> service.getRequestByUserId(null));
    }

    @Test
    void approveRequest_withNoUser_shouldCreateUserAndSendEmail() {
        var requestId = 1L;
        var adminId = 10L;
        var admin = new User();
        admin.setId(adminId);
        var sellerRequest = new SellerRequest();
        sellerRequest.setStatut(RequestStatus.EN_ATTENTE);
        sellerRequest.setUser(null);
        sellerRequest.setEmail("new@esprit.tn");
        sellerRequest.setPrenom("New");
        sellerRequest.setNom("User");

        var response = new SellerRequestResponse();

        when(sellerRequestRepository.findById(requestId)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(sellerRequestRepository.save(sellerRequest)).thenReturn(sellerRequest);
        when(sellerRequestMapper.toResponse(sellerRequest)).thenReturn(response);

        var result = service.approveRequest(requestId, adminId);

        assertNotNull(result);
        assertNotNull(sellerRequest.getUser());
        assertEquals(Role.USER, sellerRequest.getUser().getRole());
        verify(emailService).sendApprovalEmail(any(), any(), any());
    }
}
