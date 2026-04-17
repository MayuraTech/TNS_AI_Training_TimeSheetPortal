package com.tns.tms.domain.timesheet;

import com.tns.tms.domain.admin.Project;
import com.tns.tms.domain.admin.ProjectRepository;
import com.tns.tms.domain.admin.ProjectStatus;
import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.holiday.HolidayCalendarRepository;
import com.tns.tms.domain.manager.ManagerAssignment;
import com.tns.tms.domain.manager.ManagerAssignmentRepository;
import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserStatus;
import com.tns.tms.shared.dto.TimesheetEntryRequest;
import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceTest {

    @Mock private TimesheetEntryRepository entryRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ManagerAssignmentRepository managerAssignmentRepository;
    @Mock private HolidayCalendarRepository holidayCalendarRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    private OvertimeValidator overtimeValidator;
    private DayStatusComputer dayStatusComputer;
    private TimesheetService timesheetService;

    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        overtimeValidator = new OvertimeValidator();
        dayStatusComputer = new DayStatusComputer();
        timesheetService = new TimesheetService(
                entryRepository, projectRepository, managerAssignmentRepository,
                holidayCalendarRepository, overtimeValidator, dayStatusComputer,
                auditLogService, notificationService);

        testUser = User.builder()
                .id(1L).email("user@example.com").fullName("Test User")
                .passwordHash("hash").status(UserStatus.ACTIVE)
                .roles(Set.of(Role.EMPLOYEE)).build();

        testProject = Project.builder()
                .id(10L).name("Test Project").code("TP001")
                .status(ProjectStatus.ACTIVE).build();
    }

    @Test
    void submitEntry_halfHour_autoApproves() {
        TimesheetEntryRequest request = new TimesheetEntryRequest(
                10L, LocalDate.now(), "Task", null, new BigDecimal("0.5"), null);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(testProject));
        when(entryRepository.sumHoursByUserAndDate(1L, LocalDate.now())).thenReturn(null);
        when(managerAssignmentRepository.findActiveByEmployeeId(1L)).thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> {
            TimesheetEntry e = inv.getArgument(0);
            e = TimesheetEntry.builder()
                    .id(100L).user(testUser).project(testProject)
                    .date(e.getDate()).taskName(e.getTaskName())
                    .hours(e.getHours()).status(e.getStatus())
                    .autoApproved(e.isAutoApproved()).build();
            return e;
        });

        TimesheetEntry result = timesheetService.submitEntry(testUser, request);

        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.AUTO_APPROVED);
        assertThat(result.isAutoApproved()).isTrue();
    }

    @Test
    void submitEntry_overtimeNoJustification_throws() {
        // Existing 9 hours + 0.5 more = 9.5 total → requires justification
        TimesheetEntryRequest request = new TimesheetEntryRequest(
                10L, LocalDate.now(), "Task", null, new BigDecimal("0.5"), null);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(testProject));
        when(entryRepository.sumHoursByUserAndDate(1L, LocalDate.now()))
                .thenReturn(new BigDecimal("9.0"));

        assertThatThrownBy(() -> timesheetService.submitEntry(testUser, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("9 hours");
    }

    @Test
    void submitEntry_futureDate_throws() {
        TimesheetEntryRequest request = new TimesheetEntryRequest(
                10L, LocalDate.now().plusDays(1), "Task", null, new BigDecimal("4.0"), null);

        assertThatThrownBy(() -> timesheetService.submitEntry(testUser, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("future date");
    }

    @Test
    void submitEntry_beyond30Days_throws() {
        TimesheetEntryRequest request = new TimesheetEntryRequest(
                10L, LocalDate.now().minusDays(31), "Task", null, new BigDecimal("4.0"), null);

        assertThatThrownBy(() -> timesheetService.submitEntry(testUser, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("30 days");
    }

    @Test
    void editEntry_approvedStatus_throws() {
        TimesheetEntry entry = TimesheetEntry.builder()
                .id(1L).user(testUser).project(testProject)
                .date(LocalDate.now()).taskName("Task")
                .hours(new BigDecimal("4.0")).status(ApprovalStatus.APPROVED).build();

        when(entryRepository.findById(1L)).thenReturn(Optional.of(entry));

        TimesheetEntryRequest request = new TimesheetEntryRequest(
                10L, LocalDate.now(), "Updated Task", null, new BigDecimal("3.0"), null);

        assertThatThrownBy(() -> timesheetService.editEntry(testUser, 1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PENDING or CLARIFICATION_REQUESTED");
    }

    @Test
    void deleteEntry_pendingStatus_succeeds() {
        TimesheetEntry entry = TimesheetEntry.builder()
                .id(1L).user(testUser).project(testProject)
                .date(LocalDate.now()).taskName("Task")
                .hours(new BigDecimal("4.0")).status(ApprovalStatus.PENDING).build();

        when(entryRepository.findById(1L)).thenReturn(Optional.of(entry));

        assertThatCode(() -> timesheetService.deleteEntry(testUser, 1L))
                .doesNotThrowAnyException();

        verify(entryRepository).delete(entry);
    }

    @Test
    void deleteEntry_approvedStatus_throws() {
        TimesheetEntry entry = TimesheetEntry.builder()
                .id(1L).user(testUser).project(testProject)
                .date(LocalDate.now()).taskName("Task")
                .hours(new BigDecimal("4.0")).status(ApprovalStatus.APPROVED).build();

        when(entryRepository.findById(1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> timesheetService.deleteEntry(testUser, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void submitEntry_withManagerAssignment_capturesManagerId() {
        TimesheetEntryRequest request = new TimesheetEntryRequest(
                10L, LocalDate.now(), "Task", null, new BigDecimal("4.0"), null);

        User manager = User.builder().id(99L).email("mgr@example.com").fullName("Manager").build();
        ManagerAssignment assignment = ManagerAssignment.builder()
                .employee(testUser).manager(manager).build();

        when(projectRepository.findById(10L)).thenReturn(Optional.of(testProject));
        when(entryRepository.sumHoursByUserAndDate(1L, LocalDate.now())).thenReturn(null);
        when(managerAssignmentRepository.findActiveByEmployeeId(1L)).thenReturn(Optional.of(assignment));
        when(entryRepository.save(any())).thenAnswer(inv -> {
            TimesheetEntry e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });

        TimesheetEntry result = timesheetService.submitEntry(testUser, request);

        assertThat(result.getManagerIdAtSubmission()).isEqualTo(99L);
    }
}
