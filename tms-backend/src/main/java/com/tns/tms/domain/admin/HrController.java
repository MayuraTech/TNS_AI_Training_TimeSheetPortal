package com.tns.tms.domain.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
@Tag(name = "HR", description = "HR dashboard and reports")
@PreAuthorize("hasRole('HR')")
public class HrController {

    private final HrService hrService;

    public HrController(HrService hrService) {
        this.hrService = hrService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "HR org-wide dashboard KPIs")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(hrService.getDashboard());
    }

    @GetMapping("/employees/{id}/daily-summary")
    @Operation(summary = "Employee daily summary (no task detail)")
    public ResponseEntity<List<Map<String, Object>>> getEmployeeDailySummary(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(hrService.getEmployeeDailySummary(id, from, to));
    }
}
