package net.thesphynx.espritmarket.Common.Security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "Y291clN1cGVyU2VjcmV0S2V5Rm9ySldUVG9rZW5HZW5lcmF0aW9uQW5kVmFsaWRhdGlvbkluU3ByaW5nQm9vdEFwcGxpY2F0aW9u");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
    }

    @Test
    void generateToken_andExtractEmail_shouldWork() {
        var user = User.withUsername("token@example.com")
                .password("x")
                .roles("USER")
                .build();

        var token = jwtService.generateToken(user);
        var email = jwtService.extractEmail(token);

        assertEquals("token@example.com", email);
    }

    @Test
    void isTokenValid_shouldBeTrueForSameUser() {
        var user = User.withUsername("valid@example.com")
                .password("x")
                .roles("USER")
                .build();
        var token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_shouldBeFalseForDifferentUser() {
        var tokenOwner = User.withUsername("owner@example.com")
                .password("x")
                .roles("USER")
                .build();
        var differentUser = User.withUsername("other@example.com")
                .password("x")
                .roles("USER")
                .build();

        var token = jwtService.generateToken(tokenOwner);

        assertFalse(jwtService.isTokenValid(token, differentUser));
    }
}
