package com.tns.tms.domain.auth;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.shared.dto.ChangePasswordRequest;
import com.tns.tms.shared.dto.LoginRequest;
import com.tns.tms.shared.dto.LoginResponse;
import com.tns.tms.shared.exception.AccountLockedException;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import com.tns.tms.shared.exception.ValidationException;
import com.tns.tms.shared.util.PasswordValidator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String JWT_COOKIE_NAME = "tms_jwt";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenBlacklist tokenBlacklist,
                       AuditLogService auditLogService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklist = tokenBlacklist;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public LoginResponse authenticate(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (user.isLocked()) {
            throw new AccountLockedException(user.getLockedUntil());
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedAttempt(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Reset failed attempts on success
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String jwt = jwtService.generateToken(user);
        setJwtCookie(response, jwt);

        auditLogService.log(user.getId(), "LOGIN", "USER", user.getId(), null, null);
        log.info("User {} logged in successfully", user.getId());

        return LoginResponse.from(user);
    }

    @Transactional
    public void logout(String token, HttpServletResponse response) {
        try {
            String jti = jwtService.extractJti(token);
            Instant expiry = jwtService.extractExpiry(token);
            tokenBlacklist.blacklist(jti, expiry);
        } catch (Exception e) {
            log.debug("Could not extract jti from token during logout: {}", e.getMessage());
        }

        clearJwtCookie(response);

        User currentUser = getCurrentUser();
        if (currentUser != null) {
            auditLogService.log(currentUser.getId(), "LOGOUT", "USER", currentUser.getId(), null, null);
        }
    }

    @Transactional
    public void refresh(String token, HttpServletResponse response) {
        if (token == null) return;
        try {
            var claims = jwtService.validateToken(token);
            Long userId = jwtService.extractUserId(claims);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Blacklist old token
            String oldJti = claims.getId();
            Instant oldExpiry = claims.getExpiration().toInstant();
            tokenBlacklist.blacklist(oldJti, oldExpiry);

            String newJwt = jwtService.generateToken(user);
            setJwtCookie(response, newJwt);
        } catch (Exception e) {
            log.debug("Token refresh failed: {}", e.getMessage());
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }

        PasswordValidator.validate(request.newPassword());

        String oldHash = user.getPasswordHash();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setForcePasswordChange(false);
        userRepository.save(user);

        auditLogService.log(userId, "PASSWORD_CHANGED", "USER", userId, "[REDACTED]", "[REDACTED]");
        log.info("Password changed for user: {}", userId);
    }

    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
            userRepository.save(user);
            notificationService.sendAccountLockedEmail(user);
            log.warn("Account locked for user: {} after {} failed attempts", user.getId(), attempts);
        } else {
            userRepository.save(user);
        }
    }

    private void setJwtCookie(HttpServletResponse response, String jwt) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set to true in production
        cookie.setPath("/");
        cookie.setMaxAge((int) (8 * 3600)); // 8 hours
        response.addCookie(cookie);
    }

    private void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
