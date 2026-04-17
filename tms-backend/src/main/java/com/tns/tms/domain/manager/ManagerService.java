package com.tns.tms.domain.manager;

import com.tns.tms.domain.timesheet.ApprovalStatus;
import com.tns.tms.domain.timesheet.DayStatusComputer;
import com.tns.tms.domain.timesheet.TimesheetEntry;
import com.tns.tms.domain.timesheet.TimesheetEntryRepository;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ManagerService {

    private final ManagerAssignmentRepository managerAssignmentRepository;
    private final TimesheetEntryRepository entryRepository;
    private final UserRepository userRepository;
    private final DayStatusComputer dayStatusComputer;

    public ManagerService(ManagerAssignmentRepository managerAssignmentRepository,
                           TimesheetEntryRepository entryRepository,
                           UserRepository userRepository,
                           DayStatusComputer dayStatusComputer) {
        this.managerAssignmentRepository = managerAssignmentRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.dayStatusComputer = dayStatusComputer;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(Long managerId) {
        List<Long> directReportIds = managerAssignmentRepository.findDirectReportIds(managerId);
        int totalDirectReports = directReportIds.size();

        LocalDate weekStart = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        long pendingApprovals = 0;
        long approvedThisWeek = 0;

        for (Long empId : directReportIds) {
            List<TimesheetEntry> weekEntries = entryRepository.findByUserIdAndDateBetween(empId, weekStart, weekEnd);
            pendingApprovals += weekEntries.stream().filter(e -> e.getStatus() == ApprovalStatus.PENDING).count();
            approvedThisWeek += weekEntries.stream()
                    .filter(e -> e.getStatus() == ApprovalStatus.APPROVED || e.getStatus() == ApprovalStatus.AUTO_APPROVED)
                    .count();
        }

        return Map.of(
                "totalDirectReports", totalDirectReports,
                "pendingApprovals", pendingApprovals,
                "approvedThisWeek", approvedThisWeek
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTeamSummary(Long managerId) {
        List<Long> directReportIds = managerAssignmentRepository.findDirectReportIds(managerId);

        LocalDate weekStart = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        return directReportIds.stream().map(empId -> {
            User employee = userRepository.findById(empId).orElse(null);
            if (employee == null) return null;

            List<TimesheetEntry> weekEntries = entryRepository.findByUserIdAndDateBetween(empId, weekStart, weekEnd);
            BigDecimal totalHours = weekEntries.stream()
                    .map(TimesheetEntry::getHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long pendingCount = weekEntries.stream().filter(e -> e.getStatus() == ApprovalStatus.PENDING).count();

            Map<String, Object> summary = new java.util.HashMap<>();
            summary.put("employeeId", empId);
            summary.put("name", employee.getFullName());
            summary.put("hoursThisWeek", totalHours);
            summary.put("pendingCount", pendingCount);
            return summary;
        }).filter(m -> m != null).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimesheetEntry> getEmployeeWeeklyView(Long employeeId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return entryRepository.findByUserIdAndDateBetween(employeeId, weekStart, weekEnd);
    }
}
