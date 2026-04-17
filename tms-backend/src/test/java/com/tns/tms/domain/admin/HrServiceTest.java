package com.tns.tms.domain.admin;

import com.tns.tms.domain.holiday.HolidayCalendarRepository;
import com.tns.tms.domain.timesheet.ApprovalStatus;
import com.tns.tms.domain.timesheet.TimesheetEntry;
import com.tns.tms.domain.timesheet.TimesheetEntryRepository;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TimesheetEntryRepository entryRepository;
    @Mock private HolidayCalendarRepository holidayCalendarRepository;

    private HrService hrService;

    @BeforeEach
    void setUp() {
        hrService = new HrService(userRepository, entryRepository, holidayCalendarRepository);
    }

    @Test
    void getDashboard_noUsers_returnsZeroMetrics() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.findAll()).thenReturn(List.of());
        when(holidayCalendarRepository.findHolidayDatesBetween(any(), any())).thenReturn(Set.of());

        Map<String, Object> dashboard = hrService.getDashboard();

        assertThat(dashboard.get("totalEmployees")).isEqualTo(0L);
        assertThat(dashboard.get("complianceRate")).isEqualTo(0.0);
    }

    @Test
    void getDashboard_allUsersCompliant_returns100PercentCompliance() {
        User user = User.builder().id(1L).email("u@e.com").fullName("User")
                .passwordHash("h").status(UserStatus.ACTIVE).build();

        when(userRepository.count()).thenReturn(1L);
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(holidayCalendarRepository.findHolidayDatesBetween(any(), any())).thenReturn(Set.of());

        // Return entries for all work days this week
        LocalDate weekStart = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        List<TimesheetEntry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            LocalDate date = weekStart.plusDays(i);
            if (!date.isAfter(LocalDate.now())) {
                TimesheetEntry entry = TimesheetEntry.builder()
                        .id((long) i).user(user)
                        .project(Project.builder().id(1L).name("P").code("P1").build())
                        .date(date).taskName("Task").hours(new BigDecimal("8.0"))
                        .status(ApprovalStatus.APPROVED).build();
                entries.add(entry);
            }
        }
        when(entryRepository.findByUserIdAndDateBetween(eq(1L), any(), any())).thenReturn(entries);

        Map<String, Object> dashboard = hrService.getDashboard();

        assertThat(dashboard.get("totalEmployees")).isEqualTo(1L);
    }

    @Test
    void getEmployeeDailySummary_returnsAggregatedHoursWithoutTaskDetail() {
        User user = User.builder().id(1L).email("u@e.com").fullName("User")
                .passwordHash("h").status(UserStatus.ACTIVE).build();
        Project project = Project.builder().id(1L).name("P").code("P1").build();

        LocalDate today = LocalDate.now();
        List<TimesheetEntry> entries = List.of(
                TimesheetEntry.builder().id(1L).user(user).project(project)
                        .date(today).taskName("Task A").hours(new BigDecimal("4.0"))
                        .status(ApprovalStatus.APPROVED).build(),
                TimesheetEntry.builder().id(2L).user(user).project(project)
                        .date(today).taskName("Task B").hours(new BigDecimal("3.0"))
                        .status(ApprovalStatus.APPROVED).build()
        );

        when(entryRepository.findByUserIdAndDateBetween(1L, today.minusDays(7), today))
                .thenReturn(entries);

        List<Map<String, Object>> summary = hrService.getEmployeeDailySummary(
                1L, today.minusDays(7), today);

        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).get("totalHours")).isEqualTo(new BigDecimal("7.0"));
        // Verify no task names in summary (HR privacy rule)
        assertThat(summary.get(0)).doesNotContainKey("taskName");
        assertThat(summary.get(0)).doesNotContainKey("taskDescription");
    }
}
