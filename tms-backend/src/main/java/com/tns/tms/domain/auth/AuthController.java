package com.tns.tms.domain.auth;

import com.tns.tms.domain.user.User;
import com.tns.tms.shared.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Auth endpoints")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT cookie")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        LoginResponse loginResponse = authService.authenticate(request, response);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate JWT and clear cookie")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "tms_jwt", required = false) String token,
            HttpServletResponse response) {
        authService.logout(token, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Silent token refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(value = "tms_jwt", required = false) String token,
            HttpServletResponse response) {
        authService.refresh(token, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset email")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.initiateReset(request.email());
        // Always return 200 to prevent user enumeration
        return ResponseEntity.ok(Map.of("message", "If that email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please log in."));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change own password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal User currentUser) {
        authService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }
}
