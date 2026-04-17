package com.tns.tms.domain.auth;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.domain.user.UserStatus;
import com.tns.tms.shared.dto.LoginRequest;
import com.tns.tms.shared.dto.LoginResponse;
import com.tns.tms.shared.exception.AccountLockedException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private TokenBlacklist tokenBlacklist;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private HttpServletResponse httpResponse;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4); // low cost for tests
        authService = new AuthService(userRepository, passwordEncoder, jwtService,
                tokenBlacklist, auditLogService, notificationService);
    }

    private User buildActiveUser(String rawPassword) {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.EMPLOYEE))
                .failedLoginAttempts(0)
                .build();
    }

    @Test
    void authenticate_validCredentials_returnsToken() {
        User user = buildActiveUser("Password1!");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(userRepository.save(any())).thenReturn(user);

        LoginResponse response = authService.authenticate(
                new LoginRequest("user@example.com", "Password1!"), httpResponse);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("user@example.com");
        verify(auditLogService).log(eq(1L), eq("LOGIN"), eq("USER"), eq(1L), isNull(), isNull());
    }

    @Test
    void authenticate_wrongPassword_incrementsFailedAttempts() {
        User user = buildActiveUser("Password1!");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        assertThatThrownBy(() ->
                authService.authenticate(new LoginRequest("user@example.com", "WrongPass!"), httpResponse))
                .isInstanceOf(BadCredentialsException.class);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void authenticate_fifthFailedAttempt_locksAccount() {
        User user = buildActiveUser("Password1!");
        user.setFailedLoginAttempts(4); // already 4 failed
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        assertThatThrownBy(() ->
                authService.authenticate(new LoginRequest("user@example.com", "WrongPass!"), httpResponse))
                .isInstanceOf(BadCredentialsException.class);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(savedUser.getLockedUntil()).isNotNull();
        assertThat(savedUser.getLockedUntil()).isAfter(Instant.now());
        verify(notificationService).sendAccountLockedEmail(user);
    }

    @Test
    void authenticate_lockedAccount_throwsAccountLockedException() {
        User user = buildActiveUser("Password1!");
        user.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.authenticate(new LoginRequest("user@example.com", "Password1!"), httpResponse))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void authenticate_userNotFound_throwsBadCredentials() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.authenticate(new LoginRequest("nobody@example.com", "pass"), httpResponse))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logout_validToken_blacklistsJti() {
        when(jwtService.extractJti("valid-token")).thenReturn("jti-123");
        when(jwtService.extractExpiry("valid-token")).thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));

        authService.logout("valid-token", httpResponse);

        verify(tokenBlacklist).blacklist(eq("jti-123"), any(Instant.class));
    }

    @Test
    void authenticate_inactiveUser_throwsBadCredentials() {
        User user = buildActiveUser("Password1!");
        user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.authenticate(new LoginRequest("user@example.com", "Password1!"), httpResponse))
                .isInstanceOf(BadCredentialsException.class);
    }
}
