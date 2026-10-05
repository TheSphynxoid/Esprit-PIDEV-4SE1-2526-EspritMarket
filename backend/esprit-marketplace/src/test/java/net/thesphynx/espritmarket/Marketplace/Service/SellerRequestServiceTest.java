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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
        var adminEmail = "admin@esprit.tn";
        var user = new User();
        user.setId(2L);
        user.setEmail("test@esprit.tn");
        user.setName("Test User");
        user.setRole(Role.USER);
        var admin = new User();
        admin.setId(10L);
        admin.setEmail(adminEmail);
        var sellerRequest = new SellerRequest();
        sellerRequest.setStatut(RequestStatus.EN_ATTENTE);
        sellerRequest.setUser(user);
        sellerRequest.setEmail("test@esprit.tn");
        sellerRequest.setPrenom("Test");
        sellerRequest.setNom("User");

        var response = new SellerRequestResponse();

        when(sellerRequestRepository.findById(requestId)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(admin));
        when(sellerRequestRepository.save(sellerRequest)).thenReturn(sellerRequest);
        when(sellerRequestMapper.toResponse(sellerRequest)).thenReturn(response);

        var result = service.approveRequest(requestId, adminEmail);

        assertEquals(response, result);
        assertEquals(RequestStatus.APPROUVE, sellerRequest.getStatut());
        assertEquals(admin, sellerRequest.getValidatedBy());
        verify(emailService).sendApprovalEmail(any(), any(), any());
    }

    @Test
    void approveRequest_whenNotPending_shouldThrow() {
        var requestId = 1L;
        var adminEmail = "admin@esprit.tn";
        var admin = new User();
        admin.setId(10L);
        admin.setEmail(adminEmail);
        var sellerRequest = new SellerRequest();
        sellerRequest.setStatut(RequestStatus.APPROUVE);

        when(sellerRequestRepository.findById(requestId)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(admin));

        assertThrows(ConflictException.class, () -> service.approveRequest(requestId, adminEmail));
    }

    @Test
    void approveRequest_whenRequestNotFound_shouldThrow() {
        when(sellerRequestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.approveRequest(99L, "admin@esprit.tn"));
    }

    @Test
    void approveRequest_whenAdminEmailNotFound_shouldThrow() {
        var adminEmail = "ghost-admin@esprit.tn";
        var sellerRequest = new SellerRequest();
        sellerRequest.setStatut(RequestStatus.EN_ATTENTE);

        when(sellerRequestRepository.findById(1L)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.approveRequest(1L, adminEmail));
    }

    @Test
    void refuseRequest_shouldSetStatusAndReturn() {
        var requestId = 1L;
        var adminEmail = "admin@esprit.tn";
        var user = new User();
        user.setId(2L);
        user.setName("Test User");
        var admin = new User();
        admin.setId(10L);
        admin.setEmail(adminEmail);
        var sellerRequest = new SellerRequest();
        sellerRequest.setStatut(RequestStatus.EN_ATTENTE);
        sellerRequest.setUser(user);
        sellerRequest.setEmail("test@esprit.tn");
        sellerRequest.setPrenom("Test");
        sellerRequest.setNom("User");

        var response = new SellerRequestResponse();

        when(sellerRequestRepository.findById(requestId)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(admin));
        when(sellerRequestRepository.save(sellerRequest)).thenReturn(sellerRequest);
        when(sellerRequestMapper.toResponse(sellerRequest)).thenReturn(response);

        var result = service.refuseRequest(requestId, adminEmail);

        assertEquals(response, result);
        assertEquals(RequestStatus.REFUSE, sellerRequest.getStatut());
        assertEquals(admin, sellerRequest.getValidatedBy());
        verify(emailService).sendRejectionEmail(any(), any());
    }

    @Test
    void refuseRequest_whenNotPending_shouldThrow() {
        var requestId = 1L;
        var adminEmail = "admin@esprit.tn";
        var admin = new User();
        admin.setId(10L);
        admin.setEmail(adminEmail);
        var sellerRequest = new SellerRequest();
        sellerRequest.setStatut(RequestStatus.REFUSE);

        when(sellerRequestRepository.findById(requestId)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(admin));

        assertThrows(ConflictException.class, () -> service.refuseRequest(requestId, adminEmail));
    }

    @Test
    void activateSeller_withoutApprovedRequest_shouldThrowConflict() {
        var callerId = 7L;
        var user = new User();
        user.setId(callerId);
        user.setEmail("student@esprit.tn");
        user.setRole(Role.USER);

        when(userRepository.findById(callerId)).thenReturn(Optional.of(user));
        when(sellerRequestRepository.existsByUserIdAndStatut(callerId, RequestStatus.APPROUVE)).thenReturn(false);
        when(sellerRequestRepository.existsByEmailIgnoreCaseAndStatut("student@esprit.tn", RequestStatus.APPROUVE)).thenReturn(false);

        assertThrows(ConflictException.class, () -> service.activateSeller(callerId));
    }

    @Test
    void activateSeller_withApprovedRequestByUser_shouldPromoteToSeller() {
        var callerId = 7L;
        var user = new User();
        user.setId(callerId);
        user.setEmail("student@esprit.tn");
        user.setRole(Role.USER);

        when(userRepository.findById(callerId)).thenReturn(Optional.of(user));
        when(sellerRequestRepository.existsByUserIdAndStatut(callerId, RequestStatus.APPROUVE)).thenReturn(true);

        var result = service.activateSeller(callerId);

        assertEquals(Boolean.TRUE, result.get("success"));
        assertEquals("SELLER", result.get("role"));
        assertEquals(Role.SELLER, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void verifyCode_withValidCode_shouldCreateApprovedRequestBoundToCaller() {
        var callerId = 7L;
        var user = new User();
        user.setId(callerId);
        user.setEmail("student@esprit.tn");
        user.setName("Student Name");

        try {
            java.lang.reflect.Field field = SellerRequestService.class
                    .getDeclaredField("pendingVerifications");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, net.thesphynx.espritmarket.Marketplace.Entity.VerificationEntry> store =
                    (java.util.Map<String, net.thesphynx.espritmarket.Marketplace.Entity.VerificationEntry>) field.get(service);
            store.put("student@esprit.tn",
                    new net.thesphynx.espritmarket.Marketplace.Entity.VerificationEntry("12345", "student@esprit.tn"));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }

        when(userRepository.findById(callerId)).thenReturn(Optional.of(user));

        var result = service.verifyCode("student@esprit.tn", "12345", callerId);

        assertEquals(Boolean.TRUE, result.get("success"));
        verify(sellerRequestRepository).save(any(SellerRequest.class));
    }

    @Test
    void verifyCode_withForeignEmail_shouldReject() {
        var callerId = 7L;
        var user = new User();
        user.setId(callerId);
        user.setEmail("student@esprit.tn");

        try {
            java.lang.reflect.Field field = SellerRequestService.class
                    .getDeclaredField("pendingVerifications");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, net.thesphynx.espritmarket.Marketplace.Entity.VerificationEntry> store =
                    (java.util.Map<String, net.thesphynx.espritmarket.Marketplace.Entity.VerificationEntry>) field.get(service);
            store.put("other@esprit.tn",
                    new net.thesphynx.espritmarket.Marketplace.Entity.VerificationEntry("12345", "other@esprit.tn"));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }

        when(userRepository.findById(callerId)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class,
                () -> service.verifyCode("other@esprit.tn", "12345", callerId));
        verify(sellerRequestRepository, never()).save(any(SellerRequest.class));
    }

    @Test
    void verifyCode_withoutCaller_shouldReject() {
        assertThrows(BadRequestException.class,
                () -> service.verifyCode("student@esprit.tn", "12345", null));
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
    void approveRequest_withNoUser_shouldFallBackToRequestNamesAndSendEmail() {
        var requestId = 1L;
        var adminEmail = "admin@esprit.tn";
        var admin = new User();
        admin.setId(10L);
        admin.setEmail(adminEmail);
        var sellerRequest = new SellerRequest();
        sellerRequest.setStatut(RequestStatus.EN_ATTENTE);
        sellerRequest.setUser(null);
        sellerRequest.setEmail("new@esprit.tn");
        sellerRequest.setPrenom("New");
        sellerRequest.setNom("User");

        var response = new SellerRequestResponse();

        when(sellerRequestRepository.findById(requestId)).thenReturn(Optional.of(sellerRequest));
        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(admin));
        when(sellerRequestRepository.save(sellerRequest)).thenReturn(sellerRequest);
        when(sellerRequestMapper.toResponse(sellerRequest)).thenReturn(response);

        var result = service.approveRequest(requestId, adminEmail);

        assertNotNull(result);
        // Securite (A1): aucun compte fantome ne doit etre cree a l'approbation.
        assertNull(sellerRequest.getUser());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService).sendApprovalEmail(eq("new@esprit.tn"), eq("New User"), eq(0L));
    }
}
