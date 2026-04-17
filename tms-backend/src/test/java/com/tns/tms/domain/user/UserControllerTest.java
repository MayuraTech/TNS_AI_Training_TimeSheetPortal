package com.tns.tms.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.tms.config.TestSecurityConfig;
import com.tns.tms.config.WithMockTmsUser;
import com.tns.tms.domain.auth.JwtAuthFilter;
import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean UserService userService;
    @MockBean UserRepository userRepository;  // used by both controller and filter

    private User admin;
    private User employee;

    @BeforeEach
    void setUp() {
        admin    = WithMockTmsUser.admin(1L);
        employee = WithMockTmsUser.employee(2L);
    }

    @Test
    void listUsers_noFilters_returns200WithPage() throws Exception {
        when(userRepository.findWithFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(employee)));

        mockMvc.perform(get("/api/admin/users")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(2));
    }

    @Test
    void listUsers_withSearchFilter_returns200() throws Exception {
        when(userRepository.findWithFilters(eq("john"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/users")
                        .param("search", "john")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void listUsers_withStatusFilter_returns200() throws Exception {
        when(userRepository.findWithFilters(isNull(), eq(UserStatus.ACTIVE), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(employee)));

        mockMvc.perform(get("/api/admin/users")
                        .param("status", "ACTIVE")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void createUser_validData_returns200() throws Exception {
        when(userService.createUser(anyLong(), anyString(), anyString(), anySet(),
                isNull(), anyString(), anyString()))
                .thenReturn(employee);

        mockMvc.perform(post("/api/admin/users")
                        .with(WithMockTmsUser.as(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "New Employee",
                                "email", "new@example.com",
                                "roles", List.of("EMPLOYEE"),
                                "department", "Engineering",
                                "employeeId", "EMP999"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void createUser_duplicateEmail_returns400() throws Exception {
        when(userService.createUser(anyLong(), anyString(), anyString(), anySet(),
                isNull(), any(), any()))
                .thenThrow(new ValidationException("Email already in use: dup@example.com"));

        mockMvc.perform(post("/api/admin/users")
                        .with(WithMockTmsUser.as(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Dup",
                                "email", "dup@example.com",
                                "roles", List.of("EMPLOYEE"),
                                "department", "IT",
                                "employeeId", "EMP000"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void deactivateUser_returns204() throws Exception {
        doNothing().when(userService).deactivateUser(anyLong(), eq(2L));

        mockMvc.perform(post("/api/admin/users/2/deactivate")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deactivateUser_alreadyInactive_returns400() throws Exception {
        doThrow(new ValidationException("User is already inactive."))
                .when(userService).deactivateUser(anyLong(), eq(2L));

        mockMvc.perform(post("/api/admin/users/2/deactivate")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reactivateUser_returns204() throws Exception {
        doNothing().when(userService).reactivateUser(anyLong(), eq(2L));

        mockMvc.perform(post("/api/admin/users/2/reactivate")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void resetPassword_returns204() throws Exception {
        doNothing().when(userService).resetPassword(anyLong(), eq(2L));

        mockMvc.perform(post("/api/admin/users/2/reset-password")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isNoContent());
    }
}
