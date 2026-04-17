package com.tns.tms.domain.auth;

import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    private PasswordEncoder passwordEncoder;
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        passwordResetService = new PasswordResetService(
                tokenRepository, userRepository, passwordEncoder, notificationService);
    }

    @Test
    void initiateReset_existingEmail_savesTokenAndSendsEmail() {
        User user = User.builder().id(1L).email("user@example.com").fullName("Test").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        passwordResetService.initiateReset("user@example.com");

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(notificationService).sendPasswordResetEmail(eq(user), anyString());
    }

    @Test
    void initiateReset_nonExistingEmail_doesNothing() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        // Should not throw — prevents user enumeration
        assertThatCode(() -> passwordResetService.initiateReset("nobody@example.com"))
                .doesNotThrowAnyException();

        verify(tokenRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndMarksUsed() {
        User user = User.builder().id(1L).email("user@example.com").fullName("Test")
                .passwordHash("oldhash").build();
        String rawToken = "valid-raw-token";
        String tokenHash = passwordEncoder.encode(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(false)
                .build();

        when(tokenRepository.findAll()).thenReturn(List.of(token));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        passwordResetService.resetPassword(rawToken, "NewPassword1!");

        assertThat(token.isUsed()).isTrue();
        assertThat(user.getPasswordHash()).isNotEqualTo("oldhash");
    }

    @Test
    void resetPassword_expiredToken_throwsValidationException() {
        User user = User.builder().id(1L).email("user@example.com").fullName("Test").build();
        String rawToken = "expired-token";
        String tokenHash = passwordEncoder.encode(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS)) // expired
                .used(false)
                .build();

        when(tokenRepository.findAll()).thenReturn(List.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "NewPassword1!"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("invalid or has expired");
    }

    @Test
    void resetPassword_usedToken_throwsValidationException() {
        User user = User.builder().id(1L).email("user@example.com").fullName("Test").build();
        String rawToken = "used-token";
        String tokenHash = passwordEncoder.encode(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(true) // already used
                .build();

        when(tokenRepository.findAll()).thenReturn(List.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "NewPassword1!"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void resetPassword_weakPassword_throwsValidationException() {
        assertThatThrownBy(() -> passwordResetService.resetPassword("any-token", "weak"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password must be");
    }
}
