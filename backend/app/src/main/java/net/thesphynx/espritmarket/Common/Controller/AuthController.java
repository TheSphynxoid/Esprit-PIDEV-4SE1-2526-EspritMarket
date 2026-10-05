package net.thesphynx.espritmarket.Common.Controller;

import net.thesphynx.espritmarket.Common.DTO.AuthRequest;
import net.thesphynx.espritmarket.Common.DTO.AuthResponse;
import net.thesphynx.espritmarket.Common.DTO.ForgotPasswordRequest;
import net.thesphynx.espritmarket.Common.DTO.ResetPasswordRequest;
import net.thesphynx.espritmarket.Common.DTO.UserRequest;
import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication - Auth")
public class AuthController {
    private final AuthService authService;

    private static final Set<Role> ADMIN_ROLES = Set.of(Role.ADMIN, Role.ADMIN_MARKET);

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, returns JWT tokens"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful, returns JWT tokens"),
            @ApiResponse(responseCode = "400", description = "User already exists, invalid input, or restricted role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRequest request) {
        validateRegistrationRole(request.getRole());
        AuthResponse response = authService.register(toEntity(request));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send a 5-digit reset code by email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset code request accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid email format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "If the email exists, a reset code has been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using email and 5-digit code")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid code/email/password"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User info returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthResponse response = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        authService.logout(request.getToken(), request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    private void validateRegistrationRole(Role role) {
        if (role != null && ADMIN_ROLES.contains(role)) {
            throw new IllegalArgumentException("Cannot register with admin role: " + role);
        }
    }

    private User toEntity(UserRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(resolveEffectiveRegistrationRole(request.getRole()));
        return user;
    }

    private Role resolveEffectiveRegistrationRole(Role requestedRole) {
        if (requestedRole == Role.SELLER) {
            // Seller role is granted only after admin approval or successful seller verification flow.
            return Role.USER;
        }
        return requestedRole;
    }

    @lombok.Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }

    @lombok.Data
    public static class LogoutRequest {
        private String token;
        private String refreshToken;
    }
}
