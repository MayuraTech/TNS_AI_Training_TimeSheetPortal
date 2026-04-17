package com.tns.tms.domain.auth;

import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.shared.exception.ValidationException;
import com.tns.tms.shared.util.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                 UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 NotificationService notificationService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    /**
     * Initiates password reset. Always returns without error to prevent user enumeration.
     */
    @Transactional
    public void initiateReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = generateSecureToken();
            String tokenHash = passwordEncoder.encode(rawToken);

            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();
            tokenRepository.save(token);

            String resetLink = frontendUrl + "/auth/reset-password?token=" + rawToken;
            notificationService.sendPasswordResetEmail(user, resetLink);
            log.info("Password reset initiated for user: {}", user.getId());
        });
    }

    /**
     * Validates token and resets password. Token is single-use.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordValidator.validate(newPassword);

        // Find valid token by checking all non-expired, non-used tokens
        PasswordResetToken token = tokenRepository
                .findAll()
                .stream()
                .filter(t -> !t.isUsed() && !t.isExpired()
                        && passwordEncoder.matches(rawToken, t.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Reset token is invalid or has expired."));

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        log.info("Password reset completed for user: {}", user.getId());
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
