package com.tns.tms.domain.auth;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JWT blacklist for invalidated tokens on logout.
 * Entries are automatically cleaned up when they expire.
 */
@Component
public class TokenBlacklist {

    private final Map<String, Instant> blacklistedJtis = new ConcurrentHashMap<>();

    public void blacklist(String jti, Instant expiresAt) {
        blacklistedJtis.put(jti, expiresAt);
        evictExpired();
    }

    public boolean isBlacklisted(String jti) {
        Instant expiry = blacklistedJtis.get(jti);
        if (expiry == null) return false;
        if (Instant.now().isAfter(expiry)) {
            blacklistedJtis.remove(jti);
            return false;
        }
        return true;
    }

    private void evictExpired() {
        Instant now = Instant.now();
        blacklistedJtis.entrySet().removeIf(e -> now.isAfter(e.getValue()));
    }
}
