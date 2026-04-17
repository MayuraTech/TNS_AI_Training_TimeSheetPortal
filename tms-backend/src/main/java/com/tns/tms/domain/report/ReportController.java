package com.tns.tms.domain.report;

import com.tns.tms.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hr/reports")
@Tag(name = "Reports", description = "HR report generation")
@PreAuthorize("hasRole('HR')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Trigger async report generation")
    public ResponseEntity<ExportJob> generateReport(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        ExportJob job = reportService.createJob(currentUser, body.get("reportType"));
        return ResponseEntity.ok(job);
    }

    @GetMapping("/exports/{jobId}/status")
    @Operation(summary = "Poll export job status")
    public ResponseEntity<ExportJob> getJobStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(reportService.getJobStatus(jobId));
    }
}
