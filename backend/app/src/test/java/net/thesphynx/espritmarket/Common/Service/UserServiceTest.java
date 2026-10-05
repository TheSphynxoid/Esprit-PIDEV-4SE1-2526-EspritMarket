package net.thesphynx.espritmarket.Common.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService service;

    @Test
    void getAll_shouldReturnAllUsers() {
        var expected = List.of(new User(), new User());
        when(userRepository.findAll()).thenReturn(expected);

        var result = service.getAll();

        assertEquals(expected, result);
        verify(userRepository).findAll();
    }

    @Test
    void getById_whenFound_shouldReturnUser() {
        var id = 1L;
        var user = new User();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        var result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void create_shouldEncodePlainPassword() {
        var user = new User();
        user.setPassword("plain");
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(user)).thenReturn(user);

        var result = service.create(user);

        assertEquals("encoded", user.getPassword());
        assertEquals(user, result);
        verify(passwordEncoder).encode("plain");
        verify(userRepository).save(user);
    }

    @Test
    void update_whenPasswordBlank_shouldReuseExistingPassword() {
        var id = 5L;
        var existing = new User();
        existing.setPassword("encoded-old");

        var incoming = new User();
        incoming.setPassword(" ");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(incoming)).thenReturn(incoming);

        var result = service.update(id, incoming);

        assertEquals(id, incoming.getId());
        assertEquals("encoded-old", incoming.getPassword());
        assertEquals(incoming, result);
        verify(userRepository).save(incoming);
    }

    @Test
    void update_whenPasswordProvided_shouldEncodePassword() {
        var id = 6L;
        var existing = new User();
        existing.setPassword("encoded-old");

        var incoming = new User();
        incoming.setPassword("new-pass");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");
        when(userRepository.save(incoming)).thenReturn(incoming);

        service.update(id, incoming);

        assertEquals("encoded-new", incoming.getPassword());
        verify(passwordEncoder).encode("new-pass");
    }

    @Test
    void update_whenMissing_shouldThrow() {
        var id = 404L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.update(id, new User()));
    }

    @Test
    void findByEmail_shouldDelegateToRepository() {
        var email = "user@example.com";
        var user = new User();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        var result = service.findByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verify(userRepository).findByEmail(email);
    }
}
