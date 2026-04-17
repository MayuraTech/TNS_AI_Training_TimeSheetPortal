package com.tns.tms.domain.clarification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.tms.config.TestSecurityConfig;
import com.tns.tms.config.WithMockTmsUser;
import com.tns.tms.domain.admin.Project;
import com.tns.tms.domain.admin.ProjectStatus;
import com.tns.tms.domain.auth.JwtAuthFilter;
import com.tns.tms.domain.timesheet.ApprovalStatus;
import com.tns.tms.domain.timesheet.TimesheetEntry;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClarificationController.class)
@Import(TestSecurityConfig.class)
class ClarificationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean(name = "filterUserRepo") UserRepository filterUserRepo;
    @MockBean ClarificationService clarificationService;

    private User employee;
    private User manager;
    private TimesheetEntry entry;
    private ClarificationMessage message;

    @BeforeEach
    void setUp() {
        employee = WithMockTmsUser.employee(1L);
        manager  = WithMockTmsUser.manager(2L);
        Project project = Project.builder().id(10L).name("P").code("P1")
                .status(ProjectStatus.ACTIVE).build();
        entry = TimesheetEntry.builder()
                .id(100L).user(employee).project(project)
                .date(LocalDate.now()).taskName("Task")
                .hours(new BigDecimal("4.0")).status(ApprovalStatus.CLARIFICATION_REQUESTED).build();
        message = ClarificationMessage.builder()
                .id(1L).entry(entry).author(employee).message("My clarification").build();
    }

    @Test
    void getThread_returns200WithMessages() throws Exception {
        when(clarificationService.getThread(100L)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/clarifications/entries/100")
                        .with(WithMockTmsUser.as(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].message").value("My clarification"));
    }

    @Test
    void getThread_emptyThread_returns200EmptyArray() throws Exception {
        when(clarificationService.getThread(100L)).thenReturn(List.of());

        mockMvc.perform(get("/api/clarifications/entries/100")
                        .with(WithMockTmsUser.as(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void postMessage_openThread_returns200() throws Exception {
        when(clarificationService.postMessage(any(), eq(100L), anyString())).thenReturn(message);

        mockMvc.perform(post("/api/clarifications/entries/100")
                        .with(WithMockTmsUser.as(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "My clarification"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("My clarification"));
    }

    @Test
    void postMessage_closedThread_returns400() throws Exception {
        when(clarificationService.postMessage(any(), eq(100L), anyString()))
                .thenThrow(new ValidationException("Clarification thread is closed for this entry."));

        mockMvc.perform(post("/api/clarifications/entries/100")
                        .with(WithMockTmsUser.as(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "Too late"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Clarification thread is closed for this entry."));
    }

    @Test
    void postMessage_managerReplies_returns200() throws Exception {
        ClarificationMessage managerMsg = ClarificationMessage.builder()
                .id(2L).entry(entry).author(manager).message("Please clarify hours").build();
        when(clarificationService.postMessage(any(), eq(100L), anyString())).thenReturn(managerMsg);

        mockMvc.perform(post("/api/clarifications/entries/100")
                        .with(WithMockTmsUser.as(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "Please clarify hours"))))
                .andExpect(status().isOk());
    }
}
