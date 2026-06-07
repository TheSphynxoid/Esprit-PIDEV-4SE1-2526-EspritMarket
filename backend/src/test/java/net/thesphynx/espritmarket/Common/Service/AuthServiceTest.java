package net.thesphynx.espritmarket.Common.Service;

import net.thesphynx.espritmarket.Common.DTO.AuthRequest;
import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Security.JwtService;
import net.thesphynx.espritmarket.Delivery.Service.CourierService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private CourierService courierService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService service;

    @Test
    void login_shouldAuthenticateAndReturnTokenResponse() {
        var request = new AuthRequest("john@example.com", "pwd");
        var principal = org.springframework.security.core.userdetails.User
                .withUsername("john@example.com")
                .password("encoded")
                .roles("USER")
                .build();

        var user = new User();
        user.setId(7L);
        user.setEmail("john@example.com");
        user.setName("John");
        user.setRole(Role.USER);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtService.generateToken(principal, user.getId())).thenReturn("jwt-token");
        when(jwtService.generateRefreshToken(principal)).thenReturn("refresh-token");
        when(userService.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        var result = service.login(request);

        assertEquals("jwt-token", result.getToken());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("John", result.getName());
        assertEquals("USER", result.getRole());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_whenUserMissing_shouldThrow() {
        var request = new AuthRequest("missing@example.com", "pwd");
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("missing@example.com")
                .password("encoded")
                .roles("USER")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(userService.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.login(request));
    }

    @Test
    void register_shouldCreateUserAndReturnTokenResponse() {
        var user = new User();
        user.setId(11L);
        user.setEmail("new@example.com");
        user.setName("New User");
        user.setPassword("encoded");
        user.setRole(Role.SELLER);

        when(userService.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userService.create(user)).thenReturn(user);
        when(jwtService.generateToken(any(UserDetails.class), anyLong())).thenReturn("new-token");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("new-refresh-token");

        var result = service.register(user);

        assertEquals("new-token", result.getToken());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("New User", result.getName());
        assertEquals("SELLER", result.getRole());
        assertEquals("new-refresh-token", result.getRefreshToken());
    }

    @Test
    void register_whenEmailExists_shouldThrow() {
        var user = new User();
        user.setEmail("existing@example.com");

        when(userService.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> service.register(user));
    }
}
