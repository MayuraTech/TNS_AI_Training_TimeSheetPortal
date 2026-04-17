package com.tns.tms.domain.approval;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.timesheet.ApprovalStatus;
import com.tns.tms.domain.timesheet.TimesheetEntry;
import com.tns.tms.domain.timesheet.TimesheetEntryRepository;
import com.tns.tms.domain.user.User;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import com.tns.tms.shared.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final TimesheetEntryRepository entryRepository;
    private final ApprovalActionRepository approvalActionRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public ApprovalService(TimesheetEntryRepository entryRepository,
                            ApprovalActionRepository approvalActionRepository,
                            AuditLogService auditLogService,
                            NotificationService notificationService) {
        this.entryRepository = entryRepository;
        this.approvalActionRepository = approvalActionRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public TimesheetEntry approveEntry(User actor, Long entryId) {
        TimesheetEntry entry = getEntryForApproval(entryId);
        validateNotSelfApproval(actor, entry);

        if (entry.getStatus() == ApprovalStatus.APPROVED || entry.getStatus() == ApprovalStatus.AUTO_APPROVED) {
            throw new ValidationException("Entry is already approved.");
        }

        String before = entry.getStatus().name();
        entry.setStatus(ApprovalStatus.APPROVED);
        entry.setUpdatedAt(Instant.now());
        entryRepository.save(entry);

        recordAction(entry, actor, ApprovalActionType.APPROVED, null);
        auditLogService.log(actor.getId(), "ENTRY_APPROVED", "TIMESHEET_ENTRY",
                entryId, before, ApprovalStatus.APPROVED.name());

        notificationService.createInAppNotification(
                entry.getUser().getId(),
                "ENTRY_APPROVED",
                "Your timesheet entry for " + entry.getDate() + " has been approved.",
                "/employee/history"
        );

        log.info("Entry {} approved by manager {}", entryId, actor.getId());
        return entry;
    }

    @Transactional
    public TimesheetEntry rejectEntry(User actor, Long entryId, String reason) {
        if (reason == null || reason.trim().length() < 10) {
            throw new ValidationException("Rejection reason must be at least 10 characters.");
        }

        TimesheetEntry entry = getEntryForApproval(entryId);
        validateNotSelfApproval(actor, entry);

        String before = entry.getStatus().name();
        entry.setStatus(ApprovalStatus.REJECTED);
        entry.setUpdatedAt(Instant.now());
        entryRepository.save(entry);

        recordAction(entry, actor, ApprovalActionType.REJECTED, reason);
        auditLogService.log(actor.getId(), "ENTRY_REJECTED", "TIMESHEET_ENTRY",
                entryId, before, "REJECTED: " + reason);

        notificationService.createInAppNotification(
                entry.getUser().getId(),
                "ENTRY_REJECTED",
                "Your timesheet entry for " + entry.getDate() + " was rejected: " + reason,
                "/employee/history"
        );

        return entry;
    }

    @Transactional
    public TimesheetEntry requestClarification(User actor, Long entryId) {
        TimesheetEntry entry = getEntryForApproval(entryId);
        validateNotSelfApproval(actor, entry);

        String before = entry.getStatus().name();
        entry.setStatus(ApprovalStatus.CLARIFICATION_REQUESTED);
        entry.setUpdatedAt(Instant.now());
        entryRepository.save(entry);

        recordAction(entry, actor, ApprovalActionType.CLARIFICATION_REQUESTED, null);
        auditLogService.log(actor.getId(), "CLARIFICATION_REQUESTED", "TIMESHEET_ENTRY",
                entryId, before, ApprovalStatus.CLARIFICATION_REQUESTED.name());

        notificationService.createInAppNotification(
                entry.getUser().getId(),
                "CLARIFICATION_REQUESTED",
                "Clarification requested on your entry for " + entry.getDate(),
                "/employee/history/" + entryId
        );

        return entry;
    }

    @Transactional
    public int bulkApproveDay(User actor, Long employeeId, LocalDate date) {
        List<TimesheetEntry> entries = entryRepository.findByUserIdAndDate(employeeId, date)
                .stream()
                .filter(e -> e.getStatus() == ApprovalStatus.PENDING)
                .toList();

        entries.forEach(e -> approveEntry(actor, e.getId()));
        return entries.size();
    }

    @Transactional
    public int bulkRejectDay(User actor, Long employeeId, LocalDate date, String reason) {
        List<TimesheetEntry> entries = entryRepository.findByUserIdAndDate(employeeId, date)
                .stream()
                .filter(e -> e.getStatus() == ApprovalStatus.PENDING)
                .toList();

        entries.forEach(e -> rejectEntry(actor, e.getId(), reason));
        return entries.size();
    }

    private TimesheetEntry getEntryForApproval(Long entryId) {
        return entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found: " + entryId));
    }

    private void validateNotSelfApproval(User actor, TimesheetEntry entry) {
        if (entry.getUser().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Managers cannot approve their own timesheet entries.");
        }
    }

    private void recordAction(TimesheetEntry entry, User actor, ApprovalActionType action, String reason) {
        ApprovalAction approvalAction = ApprovalAction.builder()
                .entry(entry)
                .actor(actor)
                .action(action)
                .reason(reason)
                .build();
        approvalActionRepository.save(approvalAction);
    }
}
