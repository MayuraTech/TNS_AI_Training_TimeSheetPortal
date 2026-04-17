package com.tns.tms.domain.approval;

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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApprovalController.class)
@Import(TestSecurityConfig.class)
class ApprovalControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean(name = "filterUserRepo") UserRepository filterUserRepo;
    @MockBean ApprovalService approvalService;

    private User manager;
    private TimesheetEntry approvedEntry;

    @BeforeEach
    void setUp() {
        manager = WithMockTmsUser.manager(1L);
        User employee = WithMockTmsUser.employee(2L);
        Project project = Project.builder().id(10L).name("P").code("P1")
                .status(ProjectStatus.ACTIVE).build();
        approvedEntry = TimesheetEntry.builder()
                .id(100L).user(employee).project(project)
                .date(LocalDate.now()).taskName("Task")
                .hours(new BigDecimal("4.0")).status(ApprovalStatus.APPROVED).build();
    }

    @Test
    void approveEntry_returns200() throws Exception {
        when(approvalService.approveEntry(any(), eq(100L))).thenReturn(approvedEntry);

        mockMvc.perform(post("/api/approvals/entries/100/approve")
                        .with(WithMockTmsUser.as(manager)))
                .andExpect(status().isOk());
    }

    @Test
    void approveEntry_alreadyApproved_returns400() throws Exception {
        when(approvalService.approveEntry(any(), eq(100L)))
                .thenThrow(new ValidationException("Entry is already approved."));

        mockMvc.perform(post("/api/approvals/entries/100/approve")
                        .with(WithMockTmsUser.as(manager)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Entry is already approved."));
    }

    @Test
    void rejectEntry_withReason_returns200() throws Exception {
        when(approvalService.rejectEntry(any(), eq(100L), anyString())).thenReturn(approvedEntry);

        mockMvc.perform(post("/api/approvals/entries/100/reject")
                        .with(WithMockTmsUser.as(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("reason", "Hours do not match project scope"))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectEntry_noReason_returns400() throws Exception {
        when(approvalService.rejectEntry(any(), eq(100L), isNull()))
                .thenThrow(new ValidationException("Rejection reason must be at least 10 characters."));

        mockMvc.perform(post("/api/approvals/entries/100/reject")
                        .with(WithMockTmsUser.as(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestClarification_returns200() throws Exception {
        when(approvalService.requestClarification(any(), eq(100L))).thenReturn(approvedEntry);

        mockMvc.perform(post("/api/approvals/entries/100/clarify")
                        .with(WithMockTmsUser.as(manager)))
                .andExpect(status().isOk());
    }

    @Test
    void bulkApproveDay_returns200WithCount() throws Exception {
        when(approvalService.bulkApproveDay(any(), eq(2L), any())).thenReturn(3);

        mockMvc.perform(post("/api/approvals/day/2/" + LocalDate.now() + "/approve")
                        .with(WithMockTmsUser.as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(3));
    }

    @Test
    void bulkRejectDay_returns200WithCount() throws Exception {
        when(approvalService.bulkRejectDay(any(), eq(2L), any(), anyString())).thenReturn(2);

        mockMvc.perform(post("/api/approvals/day/2/" + LocalDate.now() + "/reject")
                        .with(WithMockTmsUser.as(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("reason", "Entries need correction before approval"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejected").value(2));
    }
}
