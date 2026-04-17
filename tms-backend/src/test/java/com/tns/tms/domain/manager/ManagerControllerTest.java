package com.tns.tms.domain.manager;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerController.class)
@Import(TestSecurityConfig.class)
class ManagerControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean(name = "filterUserRepo") UserRepository filterUserRepo;
    @MockBean ManagerService managerService;

    private User manager;
    private User employee;

    @BeforeEach
    void setUp() {
        manager  = WithMockTmsUser.manager(1L);
        employee = WithMockTmsUser.employee(2L);
    }

    @Test
    void getDashboard_returns200WithKpis() throws Exception {
        when(managerService.getDashboard(anyLong())).thenReturn(Map.of(
                "totalDirectReports", 5,
                "pendingApprovals", 3,
                "approvedThisWeek", 12
        ));

        mockMvc.perform(get("/api/manager/dashboard")
                        .with(WithMockTmsUser.as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDirectReports").value(5))
                .andExpect(jsonPath("$.pendingApprovals").value(3));
    }

    @Test
    void getTeamSummary_returns200WithList() throws Exception {
        when(managerService.getTeamSummary(anyLong())).thenReturn(List.of(
                Map.of("employeeId", 2, "name", "Employee", "pendingCount", 1)
        ));

        mockMvc.perform(get("/api/manager/team")
                        .with(WithMockTmsUser.as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Employee"));
    }

    @Test
    void getTeamSummary_noDirectReports_returnsEmptyList() throws Exception {
        when(managerService.getTeamSummary(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/manager/team")
                        .with(WithMockTmsUser.as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getEmployeeWeeklyView_returns200WithEntries() throws Exception {
        Project project = Project.builder().id(10L).name("P").code("P1")
                .status(ProjectStatus.ACTIVE).build();
        TimesheetEntry entry = TimesheetEntry.builder()
                .id(100L).user(employee).project(project)
                .date(LocalDate.now()).taskName("Task")
                .hours(new BigDecimal("4.0")).status(ApprovalStatus.PENDING).build();

        when(managerService.getEmployeeWeeklyView(eq(2L), any())).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/manager/team/2/week")
                        .with(WithMockTmsUser.as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    void getEmployeeWeeklyView_withWeekStartParam_returns200() throws Exception {
        when(managerService.getEmployeeWeeklyView(eq(2L), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/manager/team/2/week")
                        .param("weekStart", LocalDate.now().toString())
                        .with(WithMockTmsUser.as(manager)))
                .andExpect(status().isOk());
    }
}
