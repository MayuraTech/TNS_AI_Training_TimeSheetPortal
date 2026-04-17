package com.tns.tms.domain.auth;

import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET = "tms-test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";

    private TokenBlacklist tokenBlacklist;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        tokenBlacklist = new TokenBlacklist();
        jwtService = new JwtService(SECRET, 8L, tokenBlacklist);
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("hash")
                .roles(Set.of(Role.EMPLOYEE))
                .build();
    }

    @Test
    void generateToken_validUser_returnsNonNullToken() {
        User user = buildUser();
        String token = jwtService.generateToken(user);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void validateToken_validToken_returnsClaims() {
        User user = buildUser();
        String token = jwtService.generateToken(user);

        Claims claims = jwtService.validateToken(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
    }

    @Test
    void validateToken_blacklistedToken_throwsJwtException() {
        User user = buildUser();
        String token = jwtService.generateToken(user);
        String jti = jwtService.extractJti(token);
        tokenBlacklist.blacklist(jti, jwtService.extractExpiry(token));

        assertThatThrownBy(() -> jwtService.validateToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateToken_tamperedToken_throwsJwtException() {
        User user = buildUser();
        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.validateToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractUserId_validClaims_returnsCorrectId() {
        User user = buildUser();
        String token = jwtService.generateToken(user);
        Claims claims = jwtService.validateToken(token);

        Long userId = jwtService.extractUserId(claims);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void generateToken_differentUsers_produceDifferentTokens() {
        User user1 = buildUser();
        User user2 = User.builder().id(2L).email("other@example.com")
                .fullName("Other").passwordHash("hash").roles(Set.of(Role.MANAGER)).build();

        String token1 = jwtService.generateToken(user1);
        String token2 = jwtService.generateToken(user2);

        assertThat(token1).isNotEqualTo(token2);
    }
}
