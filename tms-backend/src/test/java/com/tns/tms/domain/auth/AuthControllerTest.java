package com.tns.tms.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.tms.config.WithMockTmsUser;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.shared.dto.*;
import com.tns.tms.shared.exception.AccountLockedException;
import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean UserRepository userRepository;
    @MockBean AuthService authService;
    @MockBean PasswordResetService passwordResetService;

    @Test
    void login_validCredentials_returns200WithUserContext() throws Exception {
        LoginResponse response = new LoginResponse(
                1L, "John Doe", "john@example.com",
                Set.of(Role.EMPLOYEE), Role.EMPLOYEE, "UTC", false);
        when(authService.authenticate(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("john@example.com", "Password1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.forcePasswordChange").value(false));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.authenticate(any(), any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("bad@example.com", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_lockedAccount_returns423() throws Exception {
        Instant lockedUntil = Instant.now().plus(10, ChronoUnit.MINUTES);
        when(authService.authenticate(any(), any()))
                .thenThrow(new AccountLockedException(lockedUntil));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("locked@example.com", "pass"))))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error").value("Account locked"))
                .andExpect(jsonPath("$.lockedUntil").exists());
    }

    @Test
    void login_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Password1!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_returns204() throws Exception {
        doNothing().when(authService).logout(any(), any());

        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void refresh_returns204() throws Exception {
        doNothing().when(authService).refresh(any(), any());

        mockMvc.perform(post("/api/auth/refresh").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void forgotPassword_alwaysReturns200() throws Exception {
        doNothing().when(passwordResetService).initiateReset(anyString());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"anyone@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void forgotPassword_nonExistentEmail_stillReturns200() throws Exception {
        doNothing().when(passwordResetService).initiateReset(anyString());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_validToken_returns200() throws Exception {
        doNothing().when(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("valid-token", "NewPass1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void resetPassword_invalidToken_returns400() throws Exception {
        doThrow(new ValidationException("Reset token is invalid or has expired."))
                .when(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("bad-token", "NewPass1!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Reset token is invalid or has expired."));
    }

    @Test
    void changePassword_validRequest_returns200() throws Exception {
        User currentUser = WithMockTmsUser.employee(1L);
        doNothing().when(authService).changePassword(anyLong(), any());

        mockMvc.perform(post("/api/auth/change-password")
                        .with(WithMockTmsUser.as(currentUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("OldPass1!", "NewPass1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
