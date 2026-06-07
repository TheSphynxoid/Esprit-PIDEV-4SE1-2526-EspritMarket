package net.thesphynx.espritmarket.Marketplace.Entity;

import java.time.LocalDateTime;

public class VerificationEntry {
    private String code;
    private String email;
    private LocalDateTime expiresAt;

    public VerificationEntry(String code, String email) {
        this.code = code;
        this.email = email;
        this.expiresAt = LocalDateTime.now().plusMinutes(10);
    }

    public String getCode() { return code; }
    public String getEmail() { return email; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}