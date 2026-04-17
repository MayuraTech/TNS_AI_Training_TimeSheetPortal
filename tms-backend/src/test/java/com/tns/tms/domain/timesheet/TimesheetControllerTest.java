package com.tns.tms.domain.timesheet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.tms.config.TestSecurityConfig;
import com.tns.tms.config.WithMockTmsUser;
import com.tns.tms.domain.admin.Project;
import com.tns.tms.domain.admin.ProjectStatus;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.domain.auth.JwtAuthFilter;
import com.tns.tms.shared.dto.TimesheetEntryRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimesheetController.class)
@Import(TestSecurityConfig.class)
class TimesheetControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean(name = "filterUserRepo") UserRepository filterUserRepo;
    @MockBean TimesheetService timesheetService;

    private User employee;
    private Project project;
    private TimesheetEntry sampleEntry;

    @BeforeEach
    void setUp() {
        employee = WithMockTmsUser.employee(1L);
        project = Project.builder().id(10L).name("Test Project").code("TP001")
                .status(ProjectStatus.ACTIVE).build();
        sampleEntry = TimesheetEntry.builder()
                .id(100L).user(employee).project(project)
                .date(LocalDate.now()).taskName("Task A")
                .hours(new BigDecimal("4.0")).status(ApprovalStatus.PENDING).build();
    }

    @Test
    void submitEntry_validRequest_returns200() throws Exception {
        when(timesheetService.submitEntry(any(), any())).thenReturn(sampleEntry);

        TimesheetEntryRequest req = new TimesheetEntryRequest(
                10L, LocalDate.now(), "Task A", "Desc", new BigDecimal("4.0"), null);

        mockMvc.perform(post("/api/timesheets/entries")
                        .with(WithMockTmsUser.as(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.taskName").value("Task A"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void submitEntry_futureDate_returns400() throws Exception {
        when(timesheetService.submitEntry(any(), any()))
                .thenThrow(new ValidationException("Cannot log time for a future date."));

        TimesheetEntryRequest req = new TimesheetEntryRequest(
                10L, LocalDate.now().plusDays(1), "Task", null, new BigDecimal("4.0"), null);

        mockMvc.perform(post("/api/timesheets/entries")
                        .with(WithMockTmsUser.as(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot log time for a future date."));
    }

    @Test
    void submitEntry_overtimeNoJustification_returns400() throws Exception {
        when(timesheetService.submitEntry(any(), any()))
                .thenThrow(new ValidationException("Please provide a reason for logging more than 9 hours."));

        TimesheetEntryRequest req = new TimesheetEntryRequest(
                10L, LocalDate.now(), "Task", null, new BigDecimal("9.5"), null);

        mockMvc.perform(post("/api/timesheets/entries")
                        .with(WithMockTmsUser.as(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitEntry_halfHour_autoApprovedInResponse() throws Exception {
        TimesheetEntry autoApproved = TimesheetEntry.builder()
                .id(101L).user(employee).project(project)
                .date(LocalDate.now()).taskName("Quick Task")
                .hours(new BigDecimal("0.5")).status(ApprovalStatus.AUTO_APPROVED)
                .autoApproved(true).build();
        when(timesheetService.submitEntry(any(), any())).thenReturn(autoApproved);

        TimesheetEntryRequest req = new TimesheetEntryRequest(
                10L, LocalDate.now(), "Quick Task", null, new BigDecimal("0.5"), null);

        mockMvc.perform(post("/api/timesheets/entries")
                        .with(WithMockTmsUser.as(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTO_APPROVED"))
                .andExpect(jsonPath("$.autoApproved").value(true));
    }

    @Test
    void editEntry_pendingEntry_returns200() throws Exception {
        when(timesheetService.editEntry(any(), eq(100L), any())).thenReturn(sampleEntry);

        TimesheetEntryRequest req = new TimesheetEntryRequest(
                10L, LocalDate.now(), "Updated Task", null, new BigDecimal("3.0"), null);

        mockMvc.perform(put("/api/timesheets/entries/100")
                        .with(WithMockTmsUser.as(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void editEntry_approvedEntry_returns400() throws Exception {
        when(timesheetService.editEntry(any(), eq(100L), any()))
                .thenThrow(new ValidationException("Only PENDING or CLARIFICATION_REQUESTED entries can be edited."));

        TimesheetEntryRequest req = new TimesheetEntryRequest(
                10L, LocalDate.now(), "Task", null, new BigDecimal("4.0"), null);

        mockMvc.perform(put("/api/timesheets/entries/100")
                        .with(WithMockTmsUser.as(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteEntry_pendingEntry_returns204() throws Exception {
        doNothing().when(timesheetService).deleteEntry(any(), eq(100L));

        mockMvc.perform(delete("/api/timesheets/entries/100")
                        .with(WithMockTmsUser.as(employee)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEntry_approvedEntry_returns400() throws Exception {
        doThrow(new ValidationException("Only PENDING entries can be deleted."))
                .when(timesheetService).deleteEntry(any(), eq(100L));

        mockMvc.perform(delete("/api/timesheets/entries/100")
                        .with(WithMockTmsUser.as(employee)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWeekly_returns200WithEntries() throws Exception {
        when(timesheetService.getWeeklyEntries(anyLong(), any())).thenReturn(List.of(sampleEntry));

        mockMvc.perform(get("/api/timesheets/week")
                        .with(WithMockTmsUser.as(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    void getHistory_returns200WithPage() throws Exception {
        when(timesheetService.getHistory(anyLong(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEntry)));

        mockMvc.perform(get("/api/timesheets/history")
                        .with(WithMockTmsUser.as(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(100));
    }

    @Test
    void getHistory_withStatusFilter_returns200() throws Exception {
        when(timesheetService.getHistory(anyLong(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/timesheets/history")
                        .param("status", "PENDING")
                        .with(WithMockTmsUser.as(employee)))
                .andExpect(status().isOk());
    }
}
