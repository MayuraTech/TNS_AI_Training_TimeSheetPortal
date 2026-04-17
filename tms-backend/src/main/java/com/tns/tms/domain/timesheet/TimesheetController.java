package com.tns.tms.domain.timesheet;

import com.tns.tms.domain.user.User;
import com.tns.tms.shared.dto.TimesheetEntryRequest;
import com.tns.tms.shared.dto.TimesheetEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/timesheets")
@Tag(name = "Timesheets", description = "Timesheet entry management")
public class TimesheetController {

    private final TimesheetService timesheetService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    @PostMapping("/entries")
    @Operation(summary = "Submit a timesheet entry")
    public ResponseEntity<TimesheetEntryResponse> submitEntry(
            @Valid @RequestBody TimesheetEntryRequest request,
            @AuthenticationPrincipal User currentUser) {
        TimesheetEntry entry = timesheetService.submitEntry(currentUser, request);
        return ResponseEntity.ok(TimesheetEntryResponse.from(entry));
    }

    @PutMapping("/entries/{id}")
    @Operation(summary = "Edit a PENDING or CLARIFICATION_REQUESTED entry")
    public ResponseEntity<TimesheetEntryResponse> editEntry(
            @PathVariable Long id,
            @Valid @RequestBody TimesheetEntryRequest request,
            @AuthenticationPrincipal User currentUser) {
        TimesheetEntry entry = timesheetService.editEntry(currentUser, id, request);
        return ResponseEntity.ok(TimesheetEntryResponse.from(entry));
    }

    @DeleteMapping("/entries/{id}")
    @Operation(summary = "Delete a PENDING entry")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        timesheetService.deleteEntry(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/week")
    @Operation(summary = "Get weekly summary")
    public ResponseEntity<List<TimesheetEntryResponse>> getWeekly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @AuthenticationPrincipal User currentUser) {
        if (weekStart == null) {
            weekStart = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        }
        List<TimesheetEntryResponse> result = timesheetService
                .getWeeklyEntries(currentUser.getId(), weekStart)
                .stream().map(TimesheetEntryResponse::from).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    @Operation(summary = "Get paginated timesheet history with filters")
    public ResponseEntity<Page<TimesheetEntryResponse>> getHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) Long projectId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        if (from == null) from = LocalDate.now().minusMonths(3);
        Page<TimesheetEntryResponse> page = timesheetService
                .getHistory(currentUser.getId(), from, to, status, projectId, pageable)
                .map(TimesheetEntryResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/missed-dates")
    @Operation(summary = "Get missed dates for current user")
    public ResponseEntity<Map<String, List<LocalDate>>> getMissedDates(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User currentUser) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        List<LocalDate> missed = timesheetService.getMissedDates(currentUser.getId(), from, to);
        return ResponseEntity.ok(Map.of("missedDates", missed));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Employee dashboard KPIs")
    public ResponseEntity<Map<String, Object>> getDashboard(@AuthenticationPrincipal User currentUser) {
        LocalDate weekStart = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);

        List<TimesheetEntry> weekEntries = timesheetService.getWeeklyEntries(currentUser.getId(), weekStart);

        double totalHours = weekEntries.stream().mapToDouble(e -> e.getHours().doubleValue()).sum();
        long pending  = weekEntries.stream().filter(e -> e.getStatus() == ApprovalStatus.PENDING).count();
        long approved = weekEntries.stream().filter(e ->
                e.getStatus() == ApprovalStatus.APPROVED || e.getStatus() == ApprovalStatus.AUTO_APPROVED).count();

        List<LocalDate> missed = timesheetService.getMissedDates(currentUser.getId(), weekStart, LocalDate.now());

        Map<String, Object> dashboard = new java.util.HashMap<>();
        dashboard.put("totalHoursThisWeek", totalHours);
        dashboard.put("pendingCount", pending);
        dashboard.put("approvedCount", approved);
        dashboard.put("missedDates", missed);
        dashboard.put("recentActivity", List.of());
        return ResponseEntity.ok(dashboard);
    }
}
