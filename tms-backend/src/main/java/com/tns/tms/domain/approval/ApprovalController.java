package com.tns.tms.domain.approval;

import com.tns.tms.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/approvals")
@Tag(name = "Approvals", description = "Manager approval workflow")
@PreAuthorize("hasRole('MANAGER')")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/entries/{id}/approve")
    @Operation(summary = "Approve a single task entry")
    public ResponseEntity<Void> approveEntry(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        approvalService.approveEntry(currentUser, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/entries/{id}/reject")
    @Operation(summary = "Reject a single task entry with reason")
    public ResponseEntity<Void> rejectEntry(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        approvalService.rejectEntry(currentUser, id, body.get("reason"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/entries/{id}/clarify")
    @Operation(summary = "Request clarification on a task entry")
    public ResponseEntity<Void> requestClarification(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        approvalService.requestClarification(currentUser, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/day/{employeeId}/{date}/approve")
    @Operation(summary = "Bulk approve all PENDING tasks for a day")
    public ResponseEntity<Map<String, Integer>> bulkApproveDay(
            @PathVariable Long employeeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal User currentUser) {
        int count = approvalService.bulkApproveDay(currentUser, employeeId, date);
        return ResponseEntity.ok(Map.of("approved", count));
    }

    @PostMapping("/day/{employeeId}/{date}/reject")
    @Operation(summary = "Bulk reject all PENDING tasks for a day")
    public ResponseEntity<Map<String, Integer>> bulkRejectDay(
            @PathVariable Long employeeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        int count = approvalService.bulkRejectDay(currentUser, employeeId, date, body.get("reason"));
        return ResponseEntity.ok(Map.of("rejected", count));
    }
}
