package com.tns.tms.domain.admin;

import com.tns.tms.domain.holiday.HolidayCalendarRepository;
import com.tns.tms.domain.timesheet.ApprovalStatus;
import com.tns.tms.domain.timesheet.TimesheetEntry;
import com.tns.tms.domain.timesheet.TimesheetEntryRepository;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.domain.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HrService {

    private final UserRepository userRepository;
    private final TimesheetEntryRepository entryRepository;
    private final HolidayCalendarRepository holidayCalendarRepository;

    public HrService(UserRepository userRepository,
                      TimesheetEntryRepository entryRepository,
                      HolidayCalendarRepository holidayCalendarRepository) {
        this.userRepository = userRepository;
        this.entryRepository = entryRepository;
        this.holidayCalendarRepository = holidayCalendarRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        long totalEmployees = userRepository.count();

        LocalDate weekStart = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        LocalDate weekEnd = weekStart.plusDays(4); // Mon-Fri

        Set<LocalDate> holidays = holidayCalendarRepository.findHolidayDatesBetween(weekStart, weekEnd);

        List<LocalDate> workDays = weekStart.datesUntil(weekEnd.plusDays(1))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY
                        && d.getDayOfWeek() != DayOfWeek.SUNDAY
                        && !holidays.contains(d)
                        && !d.isAfter(LocalDate.now()))
                .toList();

        List<User> activeUsers = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .toList();

        long compliantUsers = 0;
        BigDecimal totalHours = BigDecimal.ZERO;

        for (User user : activeUsers) {
            List<TimesheetEntry> weekEntries = entryRepository
                    .findByUserIdAndDateBetween(user.getId(), weekStart, weekEnd);

            Set<LocalDate> submittedDates = weekEntries.stream()
                    .map(TimesheetEntry::getDate).collect(Collectors.toSet());

            boolean compliant = workDays.stream().allMatch(submittedDates::contains);
            if (compliant) compliantUsers++;

            totalHours = totalHours.add(weekEntries.stream()
                    .map(TimesheetEntry::getHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        double complianceRate = activeUsers.isEmpty() ? 0.0
                : (double) compliantUsers / activeUsers.size() * 100;

        BigDecimal avgHours = activeUsers.isEmpty() ? BigDecimal.ZERO
                : totalHours.divide(BigDecimal.valueOf(activeUsers.size()), 2, RoundingMode.HALF_UP);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalEmployees", totalEmployees);
        dashboard.put("complianceRate", Math.round(complianceRate * 10.0) / 10.0);
        dashboard.put("avgHoursPerEmployee", avgHours);
        return dashboard;
    }

    /**
     * Returns aggregated daily hours for an employee — no task-level detail (HR privacy rule).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEmployeeDailySummary(Long employeeId, LocalDate from, LocalDate to) {
        List<TimesheetEntry> entries = entryRepository.findByUserIdAndDateBetween(employeeId, from, to);

        return entries.stream()
                .collect(Collectors.groupingBy(TimesheetEntry::getDate))
                .entrySet().stream()
                .map(e -> {
                    BigDecimal totalHours = e.getValue().stream()
                            .map(TimesheetEntry::getHours)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("date", e.getKey());
                    summary.put("totalHours", totalHours);
                    // No task names or descriptions — HR privacy rule
                    return summary;
                })
                .sorted(Comparator.comparing(m -> (LocalDate) m.get("date")))
                .collect(Collectors.toList());
    }
}
