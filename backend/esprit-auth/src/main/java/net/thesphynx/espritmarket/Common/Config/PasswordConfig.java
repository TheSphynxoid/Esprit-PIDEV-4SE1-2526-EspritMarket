package net.thesphynx.espritmarket.Common.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password Encryption Configuration
 * 
 * Defines the PasswordEncoder bean for hashing and validating passwords.
 * BCryptPasswordEncoder is used as it provides secure password hashing with salt.
 */
@Configuration
public class PasswordConfig {

    /**
     * Creates a BCryptPasswordEncoder bean.
     * 
     * BCrypt:
     * - Automatically generates salt during encoding
     * - Produces unique hash each time (even for same password)
     * - Resistant to rainbow table attacks
     * - Adjustable strength factor for future-proofing
     * 
     * @return BCryptPasswordEncoder with default strength (10)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
