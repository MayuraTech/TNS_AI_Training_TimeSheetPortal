package com.tns.tms.domain.approval;

import com.tns.tms.domain.admin.Project;
import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.timesheet.ApprovalStatus;
import com.tns.tms.domain.timesheet.TimesheetEntry;
import com.tns.tms.domain.timesheet.TimesheetEntryRepository;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserStatus;
import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock private TimesheetEntryRepository entryRepository;
    @Mock private ApprovalActionRepository approvalActionRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    private ApprovalService approvalService;

    private User manager;
    private User employee;
    private Project project;

    @BeforeEach
    void setUp() {
        approvalService = new ApprovalService(
                entryRepository, approvalActionRepository, auditLogService, notificationService);

        manager = User.builder().id(1L).email("mgr@example.com").fullName("Manager")
                .passwordHash("hash").status(UserStatus.ACTIVE).roles(Set.of(Role.MANAGER)).build();

        employee = User.builder().id(2L).email("emp@example.com").fullName("Employee")
                .passwordHash("hash").status(UserStatus.ACTIVE).roles(Set.of(Role.EMPLOYEE)).build();

        project = Project.builder().id(10L).name("Test Project").code("TP001").build();
    }

    private TimesheetEntry buildPendingEntry() {
        return TimesheetEntry.builder()
                .id(100L).user(employee).project(project)
                .date(LocalDate.now()).taskName("Task")
                .hours(new BigDecimal("4.0")).status(ApprovalStatus.PENDING)
                .managerIdAtSubmission(1L).build();
    }

    @Test
    void approveEntry_selfApproval_throws() {
        TimesheetEntry entry = TimesheetEntry.builder()
                .id(100L).user(manager).project(project) // same user as actor
                .date(LocalDate.now()).taskName("Task")
                .hours(new BigDecimal("4.0")).status(ApprovalStatus.PENDING).build();

        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> approvalService.approveEntry(manager, 100L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void approveEntry_validEntry_setsApproved() {
        TimesheetEntry entry = buildPendingEntry();
        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));
        when(entryRepository.save(any())).thenReturn(entry);
        when(approvalActionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TimesheetEntry result = approvalService.approveEntry(manager, 100L);

        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        verify(notificationService).createInAppNotification(eq(2L), eq("ENTRY_APPROVED"), anyString(), anyString());
    }

    @Test
    void rejectEntry_noReason_throws() {
        assertThatThrownBy(() -> approvalService.rejectEntry(manager, 100L, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("10 characters");
    }

    @Test
    void rejectEntry_tooShortReason_throws() {
        assertThatThrownBy(() -> approvalService.rejectEntry(manager, 100L, "short"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectEntry_validReason_setsRejected() {
        TimesheetEntry entry = buildPendingEntry();
        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));
        when(entryRepository.save(any())).thenReturn(entry);
        when(approvalActionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TimesheetEntry result = approvalService.rejectEntry(manager, 100L, "This is a valid rejection reason");

        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        verify(notificationService).createInAppNotification(eq(2L), eq("ENTRY_REJECTED"), anyString(), anyString());
    }

    @Test
    void requestClarification_opensClarificationThread() {
        TimesheetEntry entry = buildPendingEntry();
        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));
        when(entryRepository.save(any())).thenReturn(entry);
        when(approvalActionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TimesheetEntry result = approvalService.requestClarification(manager, 100L);

        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.CLARIFICATION_REQUESTED);
        verify(notificationService).createInAppNotification(eq(2L), eq("CLARIFICATION_REQUESTED"), anyString(), anyString());
    }

    @Test
    void approveEntry_alreadyApproved_throws() {
        TimesheetEntry entry = buildPendingEntry();
        entry.setStatus(ApprovalStatus.APPROVED);
        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> approvalService.approveEntry(manager, 100L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already approved");
    }
}
