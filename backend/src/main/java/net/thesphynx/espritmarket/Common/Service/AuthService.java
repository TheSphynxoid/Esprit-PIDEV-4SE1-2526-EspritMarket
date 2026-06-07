package net.thesphynx.espritmarket.Common.Service;

import net.thesphynx.espritmarket.Common.DTO.AuthRequest;
import net.thesphynx.espritmarket.Common.DTO.AuthResponse;
import net.thesphynx.espritmarket.Common.Entity.PasswordResetCode;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.PasswordResetCodeRepository;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Delivery.Service.CourierService;
import net.thesphynx.espritmarket.Common.Security.JwtService;
import net.thesphynx.espritmarket.Common.Security.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;
    private final CourierService courierService;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.code-expiration-minutes:10}")
    private int resetCodeExpirationMinutes;

    @Value("${spring.mail.username:no-reply@espritmarket.local}")
    private String resetMailFrom;


    public AuthService(AuthenticationManager authenticationManager,
                       TokenBlacklistService tokenBlacklistService,
                       JwtService jwtService,
                       UserService userService,
                       UserRepository userRepository,
                       CourierService courierService,
                       PasswordResetCodeRepository passwordResetCodeRepository,
                       JavaMailSender javaMailSender,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userRepository = userRepository;
        this.courierService = courierService;
        this.passwordResetCodeRepository = passwordResetCodeRepository;
        this.javaMailSender = javaMailSender;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accessToken = jwtService.generateToken(userDetails, user.getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .id(user.getId())
                .token(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .build();
    }

    public AuthResponse register(User user) {
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email already exists: " + user.getEmail());
        }

        User createdUser = userService.create(user);
        courierService.createCourierProfileIfNeeded(createdUser);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(createdUser.getEmail())
                .password(createdUser.getPassword())
                .roles(createdUser.getRole() == null ? "USER" : createdUser.getRole().name())
                .build();

        String accessToken = jwtService.generateToken(userDetails, createdUser.getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .id(createdUser.getId())
                .token(accessToken)
                .refreshToken(refreshToken)
                .email(createdUser.getEmail())
                .name(createdUser.getName())
                .role(createdUser.getRole() != null ? createdUser.getRole().name() : "USER")
                .build();
    }

    public AuthResponse getCurrentUser(String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        String email = jwtService.extractEmail(refreshToken);
        if (email == null) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole() == null ? "USER" : user.getRole().name())
                .build();

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        tokenBlacklistService.blacklistToken(refreshToken, jwtService.extractExpirationInstant(refreshToken));

        String newAccessToken = jwtService.generateToken(userDetails, user.getId());
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .id(user.getId())
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .build();
    }

    public void logout(String token, String refreshToken) {
        if (token != null && !token.isBlank()) {
            tokenBlacklistService.blacklistToken(token, jwtService.extractExpirationInstant(token));
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenBlacklistService.blacklistToken(refreshToken, jwtService.extractExpirationInstant(refreshToken));
        }
    }

    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Optional<User> user = userRepository.findByEmail(normalizedEmail);

        // Keep response generic to avoid leaking whether an account exists.
        if (user.isEmpty()) {
            return;
        }

        String code = generateFiveDigitCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(resetCodeExpirationMinutes);

        PasswordResetCode passwordResetCode = new PasswordResetCode();
        passwordResetCode.setEmail(normalizedEmail);
        passwordResetCode.setCode(code);
        passwordResetCode.setExpiresAt(expiresAt);
        passwordResetCode.setUsed(false);
        passwordResetCodeRepository.save(passwordResetCode);

        sendResetCodeEmail(normalizedEmail, code);
    }

    public void resetPassword(String email, String code, String newPassword) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Reset code is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedCode = code.trim();

        PasswordResetCode passwordResetCode = passwordResetCodeRepository
                .findTopByEmailAndCodeAndUsedFalseOrderByExpiresAtDesc(normalizedEmail, normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset code or email"));

        if (passwordResetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset code has expired");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset code or email"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetCode.setUsed(true);
        passwordResetCodeRepository.save(passwordResetCode);
    }

    private String generateFiveDigitCode() {
        return String.format("%05d", SECURE_RANDOM.nextInt(100000));
    }

    private void sendResetCodeEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resetMailFrom);
        message.setTo(email);
        message.setSubject("EspritMarket password reset code");
        message.setText("Your reset code is: " + code + "\n"
                + "It expires in " + resetCodeExpirationMinutes + " minutes.");
        javaMailSender.send(message);
    }
}
