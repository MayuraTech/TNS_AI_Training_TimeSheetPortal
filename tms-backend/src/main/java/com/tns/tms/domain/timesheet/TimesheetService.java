package com.tns.tms.domain.timesheet;

import com.tns.tms.domain.admin.Project;
import com.tns.tms.domain.admin.ProjectRepository;
import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.holiday.HolidayCalendarRepository;
import com.tns.tms.domain.manager.ManagerAssignmentRepository;
import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.user.User;
import com.tns.tms.shared.dto.TimesheetEntryRequest;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import com.tns.tms.shared.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class TimesheetService {

    private static final Logger log = LoggerFactory.getLogger(TimesheetService.class);
    private static final BigDecimal AUTO_APPROVE_THRESHOLD = new BigDecimal("1.0");
    private static final int PAST_ENTRY_LIMIT_DAYS = 30;

    private final TimesheetEntryRepository entryRepository;
    private final ProjectRepository projectRepository;
    private final ManagerAssignmentRepository managerAssignmentRepository;
    private final HolidayCalendarRepository holidayCalendarRepository;
    private final OvertimeValidator overtimeValidator;
    private final DayStatusComputer dayStatusComputer;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public TimesheetService(TimesheetEntryRepository entryRepository,
                             ProjectRepository projectRepository,
                             ManagerAssignmentRepository managerAssignmentRepository,
                             HolidayCalendarRepository holidayCalendarRepository,
                             OvertimeValidator overtimeValidator,
                             DayStatusComputer dayStatusComputer,
                             AuditLogService auditLogService,
                             NotificationService notificationService) {
        this.entryRepository = entryRepository;
        this.projectRepository = projectRepository;
        this.managerAssignmentRepository = managerAssignmentRepository;
        this.holidayCalendarRepository = holidayCalendarRepository;
        this.overtimeValidator = overtimeValidator;
        this.dayStatusComputer = dayStatusComputer;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public TimesheetEntry submitEntry(User user, TimesheetEntryRequest request) {
        validateDate(request.date());

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + request.projectId()));

        if (!project.isActive()) {
            throw new ValidationException("Cannot log time against an archived project.");
        }

        // Compute daily total including this new entry
        BigDecimal existingTotal = getExistingDailyTotal(user.getId(), request.date());
        BigDecimal newTotal = existingTotal.add(request.hours());

        overtimeValidator.validateOvertimeJustification(newTotal, request.overtimeJustification());

        Long managerIdAtSubmission = managerAssignmentRepository
                .findActiveByEmployeeId(user.getId())
                .map(ma -> ma.getManager().getId())
                .orElse(null);

        TimesheetEntry entry = TimesheetEntry.builder()
                .user(user)
                .project(project)
                .managerIdAtSubmission(managerIdAtSubmission)
                .date(request.date())
                .taskName(request.taskName())
                .taskDescription(request.taskDescription())
                .hours(request.hours())
                .overtimeJustification(request.overtimeJustification())
                .build();

        // Auto-approve if hours < 1 (i.e., 0.5 hrs)
        if (request.hours().compareTo(AUTO_APPROVE_THRESHOLD) < 0) {
            entry.setStatus(ApprovalStatus.AUTO_APPROVED);
            entry.setAutoApproved(true);
            log.info("Auto-approving entry for user {} with {} hours", user.getId(), request.hours());
        }

        TimesheetEntry saved = entryRepository.save(entry);

        auditLogService.log(user.getId(), "TIMESHEET_SUBMITTED", "TIMESHEET_ENTRY",
                saved.getId(), null, "hours=" + request.hours() + ",project=" + project.getName());

        // Notify manager (only for non-auto-approved entries)
        if (!saved.isAutoApproved() && managerIdAtSubmission != null) {
            notificationService.createInAppNotification(
                    managerIdAtSubmission,
                    "ENTRY_SUBMITTED",
                    user.getFullName() + " submitted a timesheet entry for " + request.date(),
                    "/manager/team-review/" + user.getId()
            );
        }

        return saved;
    }

    @Transactional
    public TimesheetEntry editEntry(User user, Long entryId, TimesheetEntryRequest request) {
        TimesheetEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found: " + entryId));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Cannot edit another user's entry");
        }

        if (!entry.isEditable()) {
            throw new ValidationException("Only PENDING or CLARIFICATION_REQUESTED entries can be edited.");
        }

        validateDate(request.date());

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + request.projectId()));

        // Compute daily total excluding this entry
        BigDecimal existingTotal = getExistingDailyTotal(user.getId(), request.date());
        BigDecimal totalWithoutThis = existingTotal.subtract(entry.getHours());
        BigDecimal newTotal = totalWithoutThis.add(request.hours());

        overtimeValidator.validateOvertimeJustification(newTotal, request.overtimeJustification());

        String beforeValue = "hours=" + entry.getHours() + ",status=" + entry.getStatus();

        entry.setProject(project);
        entry.setDate(request.date());
        entry.setTaskName(request.taskName());
        entry.setTaskDescription(request.taskDescription());
        entry.setHours(request.hours());
        entry.setOvertimeJustification(request.overtimeJustification());

        // If auto-approved entry edited to >= 1 hr, reset to PENDING
        if (entry.isAutoApproved() && request.hours().compareTo(AUTO_APPROVE_THRESHOLD) >= 0) {
            entry.setStatus(ApprovalStatus.PENDING);
            entry.setAutoApproved(false);
        } else if (!entry.isAutoApproved()) {
            entry.setStatus(ApprovalStatus.PENDING);
        }

        entry.setUpdatedAt(java.time.Instant.now());
        TimesheetEntry saved = entryRepository.save(entry);

        auditLogService.log(user.getId(), "TIMESHEET_EDITED", "TIMESHEET_ENTRY",
                entryId, beforeValue, "hours=" + request.hours() + ",status=" + saved.getStatus());

        // Notify manager
        if (entry.getManagerIdAtSubmission() != null) {
            notificationService.createInAppNotification(
                    entry.getManagerIdAtSubmission(),
                    "ENTRY_EDITED",
                    user.getFullName() + " edited a timesheet entry for " + request.date(),
                    "/manager/team-review/" + user.getId()
            );
        }

        return saved;
    }

    @Transactional
    public void deleteEntry(User user, Long entryId) {
        TimesheetEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found: " + entryId));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Cannot delete another user's entry");
        }

        if (!entry.isDeletable()) {
            throw new ValidationException("Only PENDING entries can be deleted.");
        }

        auditLogService.log(user.getId(), "TIMESHEET_DELETED", "TIMESHEET_ENTRY",
                entryId, "hours=" + entry.getHours() + ",date=" + entry.getDate(), null);

        entryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public List<TimesheetEntry> getWeeklyEntries(Long userId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return entryRepository.findByUserIdAndDateBetween(userId, weekStart, weekEnd);
    }

    @Transactional(readOnly = true)
    public Page<TimesheetEntry> getHistory(Long userId, LocalDate from, LocalDate to,
                                            ApprovalStatus status, Long projectId, Pageable pageable) {
        return entryRepository.findWithFilters(userId, from, to, status, projectId, pageable);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getMissedDates(Long userId, LocalDate from, LocalDate to) {
        Set<LocalDate> holidays = holidayCalendarRepository.findHolidayDatesBetween(from, to);
        List<LocalDate> submittedDates = entryRepository.findDistinctDatesByUserAndRange(userId, from, to);

        return from.datesUntil(to.plusDays(1))
                .filter(date -> {
                    DayOfWeek dow = date.getDayOfWeek();
                    return dow != DayOfWeek.SATURDAY
                            && dow != DayOfWeek.SUNDAY
                            && !holidays.contains(date)
                            && !submittedDates.contains(date)
                            && date.isBefore(LocalDate.now());
                })
                .toList();
    }

    private void validateDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            throw new ValidationException("Cannot log time for a future date.");
        }
        if (date.isBefore(today.minusDays(PAST_ENTRY_LIMIT_DAYS))) {
            throw new ValidationException(
                "Cannot log time for dates more than " + PAST_ENTRY_LIMIT_DAYS + " days in the past.");
        }
    }

    private BigDecimal getExistingDailyTotal(Long userId, LocalDate date) {
        BigDecimal total = entryRepository.sumHoursByUserAndDate(userId, date);
        return total != null ? total : BigDecimal.ZERO;
    }
}
