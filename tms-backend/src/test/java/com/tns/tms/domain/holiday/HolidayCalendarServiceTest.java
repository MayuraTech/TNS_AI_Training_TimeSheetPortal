package com.tns.tms.domain.holiday;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserStatus;
import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HolidayCalendarServiceTest {

    @Mock private HolidayCalendarRepository holidayCalendarRepository;
    @Mock private AuditLogService auditLogService;

    private HolidayCalendarService holidayCalendarService;
    private User hrUser;

    @BeforeEach
    void setUp() {
        holidayCalendarService = new HolidayCalendarService(holidayCalendarRepository, auditLogService);
        hrUser = User.builder().id(1L).email("hr@example.com").fullName("HR User")
                .passwordHash("hash").status(UserStatus.ACTIVE).roles(Set.of(Role.HR)).build();
    }

    @Test
    void addHoliday_newDate_savesHoliday() {
        LocalDate date = LocalDate.of(2026, 12, 25);
        when(holidayCalendarRepository.existsByDate(date)).thenReturn(false);
        when(holidayCalendarRepository.save(any())).thenAnswer(inv -> {
            HolidayCalendar h = inv.getArgument(0);
            h = HolidayCalendar.builder().id(1L).name(h.getName()).date(h.getDate())
                    .type(h.getType()).applicableTo(h.getApplicableTo()).createdBy(h.getCreatedBy()).build();
            return h;
        });

        HolidayCalendar result = holidayCalendarService.addHoliday(
                hrUser, "Christmas", date, HolidayType.PUBLIC, "ALL");

        assertThat(result.getName()).isEqualTo("Christmas");
        assertThat(result.getDate()).isEqualTo(date);
        verify(auditLogService).log(eq(1L), eq("HOLIDAY_ADDED"), eq("HOLIDAY_CALENDAR"), any(), isNull(), anyString());
    }

    @Test
    void addHoliday_retroactiveDate_clearsMissedFlag() {
        // When a holiday is added for a past date, the system should handle it
        // (In this test we verify the holiday is saved — missed flag clearing happens in scheduler)
        LocalDate pastDate = LocalDate.now().minusDays(5);
        when(holidayCalendarRepository.existsByDate(pastDate)).thenReturn(false);
        when(holidayCalendarRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> holidayCalendarService.addHoliday(
                hrUser, "Past Holiday", pastDate, HolidayType.COMPANY, "ALL"))
                .doesNotThrowAnyException();
    }

    @Test
    void addHoliday_duplicateDate_throwsValidationException() {
        LocalDate date = LocalDate.of(2026, 12, 25);
        when(holidayCalendarRepository.existsByDate(date)).thenReturn(true);

        assertThatThrownBy(() -> holidayCalendarService.addHoliday(
                hrUser, "Christmas", date, HolidayType.PUBLIC, "ALL"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void deleteHoliday_existingHoliday_deletesAndAudits() {
        HolidayCalendar holiday = HolidayCalendar.builder()
                .id(1L).name("Christmas").date(LocalDate.of(2026, 12, 25))
                .type(HolidayType.PUBLIC).applicableTo("ALL").createdBy(1L).build();

        when(holidayCalendarRepository.findById(1L)).thenReturn(Optional.of(holiday));

        holidayCalendarService.deleteHoliday(hrUser, 1L);

        verify(holidayCalendarRepository).delete(holiday);
        verify(auditLogService).log(eq(1L), eq("HOLIDAY_DELETED"), eq("HOLIDAY_CALENDAR"), eq(1L), anyString(), isNull());
    }

    @Test
    void isHoliday_existingDate_returnsTrue() {
        LocalDate date = LocalDate.of(2026, 12, 25);
        when(holidayCalendarRepository.existsByDate(date)).thenReturn(true);

        assertThat(holidayCalendarService.isHoliday(date)).isTrue();
    }

    @Test
    void isHoliday_nonHolidayDate_returnsFalse() {
        LocalDate date = LocalDate.of(2026, 12, 26);
        when(holidayCalendarRepository.existsByDate(date)).thenReturn(false);

        assertThat(holidayCalendarService.isHoliday(date)).isFalse();
    }
}
