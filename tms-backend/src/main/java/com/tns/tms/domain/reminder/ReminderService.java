package com.tns.tms.domain.reminder;

import com.tns.tms.domain.holiday.HolidayCalendarRepository;
import com.tns.tms.domain.manager.ManagerAssignmentRepository;
import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.timesheet.ApprovalStatus;
import com.tns.tms.domain.timesheet.TimesheetEntry;
import com.tns.tms.domain.timesheet.TimesheetEntryRepository;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.domain.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final UserRepository userRepository;
    private final TimesheetEntryRepository entryRepository;
    private final ManagerAssignmentRepository managerAssignmentRepository;
    private final HolidayCalendarRepository holidayCalendarRepository;
    private final NotificationService notificationService;
    private final ReminderLogRepository reminderLogRepository;

    public ReminderService(UserRepository userRepository,
                            TimesheetEntryRepository entryRepository,
                            ManagerAssignmentRepository managerAssignmentRepository,
                            HolidayCalendarRepository holidayCalendarRepository,
                            NotificationService notificationService,
                            ReminderLogRepository reminderLogRepository) {
        this.userRepository = userRepository;
        this.entryRepository = entryRepository;
        this.managerAssignmentRepository = managerAssignmentRepository;
        this.holidayCalendarRepository = holidayCalendarRepository;
        this.notificationService = notificationService;
        this.reminderLogRepository = reminderLogRepository;
    }

    /**
     * HR sends missing-entry reminder to all employees with missed dates.
     */
    @Transactional
    public int sendMissingEntryReminderOrgWide(User sender) {
        LocalDate weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        LocalDate today = LocalDate.now();
        Set<LocalDate> holidays = holidayCalendarRepository.findHolidayDatesBetween(weekStart, today);

        List<User> usersWithMissedDates = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> hasMissedDates(u.getId(), weekStart, today, holidays))
                .toList();

        usersWithMissedDates.forEach(u -> {
            notificationService.createInAppNotification(
                    u.getId(), "MISSING_ENTRY_REMINDER",
                    "Reminder: You have missing timesheet entries for this week.",
                    "/employee/log-time"
            );
        });

        logReminder(sender, "HR", "ALL_EMPLOYEES_WITH_MISSED_DATES", usersWithMissedDates.size());
        return usersWithMissedDates.size();
    }

    /**
     * Manager sends missing-entry reminder to their direct reports.
     */
    @Transactional
    public int sendMissingEntryReminderToDirectReports(User manager) {
        LocalDate weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        LocalDate today = LocalDate.now();
        Set<LocalDate> holidays = holidayCalendarRepository.findHolidayDatesBetween(weekStart, today);

        List<Long> directReportIds = managerAssignmentRepository.findDirectReportIds(manager.getId());

        List<User> usersWithMissedDates = directReportIds.stream()
                .map(id -> userRepository.findById(id).orElse(null))
                .filter(u -> u != null && u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> hasMissedDates(u.getId(), weekStart, today, holidays))
                .toList();

        usersWithMissedDates.forEach(u -> {
            notificationService.createInAppNotification(
                    u.getId(), "MISSING_ENTRY_REMINDER",
                    "Reminder from your manager: Please submit missing timesheet entries.",
                    "/employee/log-time"
            );
        });

        logReminder(manager, "MANAGER", "DIRECT_REPORTS_WITH_MISSED_DATES", usersWithMissedDates.size());
        return usersWithMissedDates.size();
    }

    /**
     * HR sends pending-approval reminder to managers with items > 2 business days old.
     */
    @Transactional
    public int sendPendingApprovalReminder(User sender) {
        LocalDate cutoff = LocalDate.now().minusDays(2);

        List<TimesheetEntry> stalePending = entryRepository.findAll().stream()
                .filter(e -> e.getStatus() == ApprovalStatus.PENDING)
                .filter(e -> e.getDate().isBefore(cutoff))
                .toList();

        Set<Long> managerIds = stalePending.stream()
                .filter(e -> e.getManagerIdAtSubmission() != null)
                .map(TimesheetEntry::getManagerIdAtSubmission)
                .collect(Collectors.toSet());

        managerIds.forEach(managerId -> {
            notificationService.createInAppNotification(
                    managerId, "PENDING_APPROVAL_REMINDER",
                    "You have timesheet entries pending approval for more than 2 business days.",
                    "/manager/team-review"
            );
        });

        logReminder(sender, "HR", "MANAGERS_WITH_STALE_PENDING", managerIds.size());
        return managerIds.size();
    }

    private boolean hasMissedDates(Long userId, LocalDate from, LocalDate to, Set<LocalDate> holidays) {
        List<LocalDate> submittedDates = entryRepository.findDistinctDatesByUserAndRange(userId, from, to);
        return from.datesUntil(to.plusDays(1))
                .anyMatch(date -> {
                    DayOfWeek dow = date.getDayOfWeek();
                    return dow != DayOfWeek.SATURDAY
                            && dow != DayOfWeek.SUNDAY
                            && !holidays.contains(date)
                            && !submittedDates.contains(date);
                });
    }

    private void logReminder(User sender, String senderRole, String recipientType, int count) {
        ReminderLog reminderLog = ReminderLog.builder()
                .sentBy(sender.getId())
                .senderRole(senderRole)
                .recipientType(recipientType)
                .recipientCount(count)
                .build();
        reminderLogRepository.save(reminderLog);
        log.info("Reminder sent by {} ({}): {} recipients of type {}", sender.getId(), senderRole, count, recipientType);
    }
}
