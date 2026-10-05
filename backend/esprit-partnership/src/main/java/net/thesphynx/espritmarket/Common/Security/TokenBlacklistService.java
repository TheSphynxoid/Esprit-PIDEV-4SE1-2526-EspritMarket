package net.thesphynx.espritmarket.Common.Security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {
    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Instant expiration) {
        blacklistedTokens.put(token, expiration);
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
}
