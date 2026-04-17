package com.tns.tms.domain.manager;

import com.tns.tms.shared.dto.TimesheetEntryResponse;
import com.tns.tms.domain.timesheet.TimesheetEntry;
import com.tns.tms.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
@Tag(name = "Manager", description = "Manager dashboard and team review")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Manager dashboard KPIs")
    public ResponseEntity<Map<String, Object>> getDashboard(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(managerService.getDashboard(currentUser.getId()));
    }

    @GetMapping("/team")
    @Operation(summary = "List direct reports with status summary")
    public ResponseEntity<List<Map<String, Object>>> getTeamSummary(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(managerService.getTeamSummary(currentUser.getId()));
    }

    @GetMapping("/team/{employeeId}/week")
    @Operation(summary = "Get employee weekly view for review")
    public ResponseEntity<List<TimesheetEntryResponse>> getEmployeeWeeklyView(
            @PathVariable Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        if (weekStart == null) {
            weekStart = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        }
        List<TimesheetEntryResponse> result = managerService.getEmployeeWeeklyView(employeeId, weekStart)
                .stream().map(TimesheetEntryResponse::from).toList();
        return ResponseEntity.ok(result);
    }
}
