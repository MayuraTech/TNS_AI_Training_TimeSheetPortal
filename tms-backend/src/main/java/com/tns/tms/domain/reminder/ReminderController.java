package com.tns.tms.domain.reminder;

import com.tns.tms.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Reminders", description = "Reminder management for HR and Managers")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping("/hr/reminders/missing")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "HR: Send missing-entry reminder to all employees")
    public ResponseEntity<Map<String, Object>> sendMissingEntryReminderOrgWide(
            @AuthenticationPrincipal User currentUser) {
        int count = reminderService.sendMissingEntryReminderOrgWide(currentUser);
        return ResponseEntity.ok(Map.of("recipientCount", count, "message", "Reminders sent to " + count + " employees"));
    }

    @PostMapping("/hr/reminders/pending-approvals")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "HR: Send pending-approval reminder to managers")
    public ResponseEntity<Map<String, Object>> sendPendingApprovalReminder(
            @AuthenticationPrincipal User currentUser) {
        int count = reminderService.sendPendingApprovalReminder(currentUser);
        return ResponseEntity.ok(Map.of("recipientCount", count, "message", "Reminders sent to " + count + " managers"));
    }

    @PostMapping("/manager/reminders/missing")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Manager: Send missing-entry reminder to direct reports")
    public ResponseEntity<Map<String, Object>> sendMissingEntryReminderToDirectReports(
            @AuthenticationPrincipal User currentUser) {
        int count = reminderService.sendMissingEntryReminderToDirectReports(currentUser);
        return ResponseEntity.ok(Map.of("recipientCount", count, "message", "Reminders sent to " + count + " direct reports"));
    }
}
