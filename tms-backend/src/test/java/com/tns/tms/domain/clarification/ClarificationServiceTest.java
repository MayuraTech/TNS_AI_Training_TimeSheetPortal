package com.tns.tms.domain.clarification;

import com.tns.tms.domain.admin.Project;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClarificationServiceTest {

    @Mock private ClarificationRepository clarificationRepository;
    @Mock private TimesheetEntryRepository entryRepository;
    @Mock private NotificationService notificationService;

    private ClarificationService clarificationService;

    private User employee;
    private User manager;
    private Project project;

    @BeforeEach
    void setUp() {
        clarificationService = new ClarificationService(
                clarificationRepository, entryRepository, notificationService);

        employee = User.builder().id(1L).email("emp@example.com").fullName("Employee")
                .passwordHash("hash").status(UserStatus.ACTIVE).roles(Set.of(Role.EMPLOYEE)).build();

        manager = User.builder().id(2L).email("mgr@example.com").fullName("Manager")
                .passwordHash("hash").status(UserStatus.ACTIVE).roles(Set.of(Role.MANAGER)).build();

        project = Project.builder().id(10L).name("Test Project").code("TP001").build();
    }

    private TimesheetEntry buildEntry(ApprovalStatus status) {
        return TimesheetEntry.builder()
                .id(100L).user(employee).project(project)
                .date(LocalDate.now()).taskName("Task")
                .hours(new BigDecimal("4.0")).status(status)
                .managerIdAtSubmission(2L).build();
    }

    @Test
    void postMessage_closedThread_throws() {
        TimesheetEntry entry = buildEntry(ApprovalStatus.APPROVED);
        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> clarificationService.postMessage(employee, 100L, "Hello"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void postMessage_rejectedThread_throws() {
        TimesheetEntry entry = buildEntry(ApprovalStatus.REJECTED);
        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> clarificationService.postMessage(manager, 100L, "Hello"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void postMessage_openThread_appends() {
        TimesheetEntry entry = buildEntry(ApprovalStatus.CLARIFICATION_REQUESTED);
        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));
        when(clarificationRepository.save(any())).thenAnswer(inv -> {
            ClarificationMessage msg = inv.getArgument(0);
            msg = ClarificationMessage.builder()
                    .id(1L).entry(entry).author(employee).message(msg.getMessage()).build();
            return msg;
        });

        ClarificationMessage result = clarificationService.postMessage(employee, 100L, "My clarification");

        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("My clarification");
        verify(clarificationRepository).save(any());
    }

    @Test
    void postMessage_employeeReplies_notifiesManager() {
        TimesheetEntry entry = buildEntry(ApprovalStatus.CLARIFICATION_REQUESTED);
        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));
        when(clarificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clarificationService.postMessage(employee, 100L, "My clarification response");

        verify(notificationService).createInAppNotification(eq(2L), eq("CLARIFICATION_REPLY"), anyString(), anyString());
    }

    @Test
    void getThread_returnsMessages() {
        TimesheetEntry entry = buildEntry(ApprovalStatus.CLARIFICATION_REQUESTED);
        when(entryRepository.findById(100L)).thenReturn(Optional.of(entry));
        when(clarificationRepository.findByEntryIdOrderByCreatedAtAsc(100L)).thenReturn(List.of());

        List<ClarificationMessage> result = clarificationService.getThread(100L);

        assertThat(result).isNotNull();
    }
}
