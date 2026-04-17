package com.tns.tms.domain.auth;

import com.tns.tms.domain.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token generation and validation using HS256 (symmetric key).
 * Uses jti claim for blacklisting on logout.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey secretKey;
    private final long expirationHours;
    private final TokenBlacklist tokenBlacklist;

    public JwtService(
            @Value("${app.jwt.secret:tms-default-secret-key-must-be-at-least-256-bits-long-for-hs256}") String secret,
            @Value("${app.jwt.expiration-hours:8}") long expirationHours,
            TokenBlacklist tokenBlacklist) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationHours = expirationHours;
        this.tokenBlacklist = tokenBlacklist;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().stream().map(Enum::name).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            if (tokenBlacklist.isBlacklisted(jti)) {
                throw new JwtException("Token has been invalidated");
            }

            return claims;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            throw e;
        }
    }

    public String extractJti(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getId();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getId();
        }
    }

    public Instant extractExpiry(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration()
                    .toInstant();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getExpiration().toInstant();
        }
    }

    public Long extractUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }
}
