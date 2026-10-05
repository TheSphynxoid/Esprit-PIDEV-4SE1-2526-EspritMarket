package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Delivery.Dto.AdminCourierInfoResponse;
import net.thesphynx.espritmarket.Delivery.Dto.CourierInterviewDateRequest;
import net.thesphynx.espritmarket.Delivery.Dto.CourierInterviewDateResponse;
import net.thesphynx.espritmarket.Delivery.Dto.CourierProfileResponse;
import net.thesphynx.espritmarket.Delivery.Dto.CourierProfileUpdateRequest;
import net.thesphynx.espritmarket.Delivery.Entity.Courier;
import net.thesphynx.espritmarket.Delivery.Entity.CourierStatus;
import net.thesphynx.espritmarket.Delivery.Entity.Vehicule;
import net.thesphynx.espritmarket.Delivery.Repository.ICourierRepository;
import net.thesphynx.espritmarket.Delivery.Repository.IVehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourierServiceTest {

    @Mock
    private ICourierRepository courierRepository;

    @Mock
    private IVehiculeRepository vehiculeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender javaMailSender;

    private CourierService service;

    @BeforeEach
    void setUp() {
        service = new CourierService(courierRepository, vehiculeRepository, userRepository, javaMailSender);
    }

    private User buildCourierUser() {
        var user = new User();
        user.setId(1L);
        user.setEmail("courier@test.com");
        user.setName("Jean Dupont");
        user.setRole(Role.COURIER);
        return user;
    }

    private Courier buildCourier(User user) {
        return Courier.builder().id(10L).user(user).status(CourierStatus.PENDING).build();
    }

    @Test
    void getMyProfile_shouldReturnMappedResponse() {
        var user = buildCourierUser();
        var courier = buildCourier(user);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(courierRepository.findByUserId(user.getId())).thenReturn(Optional.of(courier));

        var result = service.getMyProfile(user.getEmail());

        assertEquals(courier.getId(), result.getId());
        assertEquals(user.getId(), result.getUserId());
    }

    @Test
    void getMyProfile_whenNotCourier_shouldThrow() {
        var user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> service.getMyProfile(user.getEmail()));
    }

    @Test
    void getMyProfile_whenUserNotFound_shouldThrow() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getMyProfile("missing@test.com"));
    }

    @Test
    void updateMyPhoneNumber_shouldSaveAndReturn() {
        var user = buildCourierUser();
        var courier = buildCourier(user);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(courierRepository.findByUserId(user.getId())).thenReturn(Optional.of(courier));
        when(courierRepository.save(courier)).thenReturn(courier);

        var request = CourierProfileUpdateRequest.builder().phoneNumber("+216 12 345 678").build();
        var result = service.updateMyPhoneNumber(user.getEmail(), request);

        assertNotNull(result);
        verify(courierRepository).save(courier);
    }

    @Test
    void createCourierProfileIfNeeded_whenCourier_shouldCreateIfMissing() {
        var user = buildCourierUser();

        when(courierRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createCourierProfileIfNeeded(user);

        verify(courierRepository).save(any(Courier.class));
    }

    @Test
    void createCourierProfileIfNeeded_whenNotCourier_shouldDoNothing() {
        var user = new User();
        user.setId(1L);
        user.setRole(Role.USER);

        service.createCourierProfileIfNeeded(user);
    }

    @Test
    void createCourierProfileIfNeeded_whenNull_shouldDoNothing() {
        service.createCourierProfileIfNeeded(null);
    }

    @Test
    void createCourierProfileIfNeeded_whenAlreadyExists_shouldNotCreate() {
        var user = buildCourierUser();
        var existing = buildCourier(user);

        when(courierRepository.findByUserId(user.getId())).thenReturn(Optional.of(existing));

        service.createCourierProfileIfNeeded(user);
    }

    @Test
    void updateCourierStatus_shouldUpdateAndReturn() {
        var user = buildCourierUser();
        var courier = buildCourier(user);

        when(courierRepository.findWithUserById(courier.getId())).thenReturn(Optional.of(courier));
        when(courierRepository.save(courier)).thenReturn(courier);
        when(vehiculeRepository.findByUserId(user.getId())).thenReturn(List.of());

        var result = service.updateCourierStatus(courier.getId(), CourierStatus.ACCEPTED);

        assertEquals(CourierStatus.ACCEPTED, courier.getStatus());
        assertNotNull(result);
    }

    @Test
    void createInterviewDate_shouldSetAndReturn() {
        var user = buildCourierUser();
        var courier = buildCourier(user);
        var interviewDate = LocalDateTime.now().plusDays(3);
        var request = CourierInterviewDateRequest.builder()
                .interviewDate(interviewDate)
                .sendEmail(false)
                .build();

        when(courierRepository.findWithUserById(courier.getId())).thenReturn(Optional.of(courier));
        when(courierRepository.save(courier)).thenReturn(courier);

        var result = service.createInterviewDate(courier.getId(), request);

        assertEquals(courier.getId(), result.getCourierId());
        assertEquals(interviewDate, courier.getInterviewDate());
    }

    @Test
    void createInterviewDate_whenAlreadySet_shouldThrow() {
        var user = buildCourierUser();
        var courier = buildCourier(user);
        courier.setInterviewDate(LocalDateTime.now().plusDays(1));

        var request = CourierInterviewDateRequest.builder()
                .interviewDate(LocalDateTime.now().plusDays(3))
                .sendEmail(false)
                .build();

        when(courierRepository.findWithUserById(courier.getId())).thenReturn(Optional.of(courier));

        assertThrows(IllegalArgumentException.class,
                () -> service.createInterviewDate(courier.getId(), request));
    }

    @Test
    void createInterviewDate_withNullDate_shouldThrow() {
        var user = buildCourierUser();
        var courier = buildCourier(user);
        var request = CourierInterviewDateRequest.builder()
                .interviewDate(null)
                .sendEmail(false)
                .build();

        when(courierRepository.findWithUserById(courier.getId())).thenReturn(Optional.of(courier));

        assertThrows(IllegalArgumentException.class,
                () -> service.createInterviewDate(courier.getId(), request));
    }

    @Test
    void updateInterviewDate_shouldOverwriteAndReturn() {
        var user = buildCourierUser();
        var courier = buildCourier(user);
        var newDate = LocalDateTime.now().plusDays(5);
        var request = CourierInterviewDateRequest.builder()
                .interviewDate(newDate)
                .sendEmail(false)
                .build();

        when(courierRepository.findWithUserById(courier.getId())).thenReturn(Optional.of(courier));
        when(courierRepository.save(courier)).thenReturn(courier);

        var result = service.updateInterviewDate(courier.getId(), request);

        assertEquals(newDate, courier.getInterviewDate());
        assertNotNull(result);
    }

    @Test
    void getInterviewDate_shouldReturnResponse() {
        var user = buildCourierUser();
        var courier = buildCourier(user);
        var date = LocalDateTime.now().plusDays(2);
        courier.setInterviewDate(date);

        when(courierRepository.findWithUserById(courier.getId())).thenReturn(Optional.of(courier));

        var result = service.getInterviewDate(courier.getId());

        assertEquals(courier.getId(), result.getCourierId());
        assertEquals(date, result.getInterviewDate());
    }

    @Test
    void deleteInterviewDate_shouldClearDate() {
        var user = buildCourierUser();
        var courier = buildCourier(user);
        courier.setInterviewDate(LocalDateTime.now().plusDays(1));

        when(courierRepository.findWithUserById(courier.getId())).thenReturn(Optional.of(courier));
        when(courierRepository.save(courier)).thenReturn(courier);

        service.deleteInterviewDate(courier.getId());

        verify(courierRepository).save(courier);
    }

    @Test
    void deleteInterviewDate_whenNoDate_shouldThrow() {
        var user = buildCourierUser();
        var courier = buildCourier(user);

        when(courierRepository.findWithUserById(courier.getId())).thenReturn(Optional.of(courier));

        assertThrows(IllegalArgumentException.class,
                () -> service.deleteInterviewDate(courier.getId()));
    }

    @Test
    void getCouriersForAdminTable_shouldReturnMappedList() {
        var user = buildCourierUser();
        var courier = buildCourier(user);

        when(courierRepository.findAllBy()).thenReturn(List.of(courier));
        when(vehiculeRepository.findByUserId(user.getId())).thenReturn(List.of());

        var result = service.getCouriersForAdminTable();

        assertEquals(1, result.size());
        assertEquals(courier.getId(), result.get(0).getCourierId());
    }

    @Test
    void createInterviewDate_withSendEmail_shouldSendMail() {
        var user = buildCourierUser();
        var courier = buildCourier(user);
        var interviewDate = LocalDateTime.now().plusDays(3);
        var request = CourierInterviewDateRequest.builder()
                .interviewDate(interviewDate)
                .sendEmail(true)
                .build();

        when(courierRepository.findWithUserById(courier.getId())).thenReturn(Optional.of(courier));
        when(courierRepository.save(courier)).thenReturn(courier);

        service.createInterviewDate(courier.getId(), request);

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }
}
