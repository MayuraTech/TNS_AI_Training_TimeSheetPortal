package com.tns.tms.domain.report;

import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.timesheet.TimesheetEntryRepository;
import com.tns.tms.domain.user.User;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ExportJobRepository exportJobRepository;
    private final NotificationService notificationService;
    private final TimesheetEntryRepository entryRepository;

    public ReportService(ExportJobRepository exportJobRepository,
                          NotificationService notificationService,
                          TimesheetEntryRepository entryRepository) {
        this.exportJobRepository = exportJobRepository;
        this.notificationService = notificationService;
        this.entryRepository = entryRepository;
    }

    @Transactional
    public ExportJob createJob(User requestedBy, String reportType) {
        ExportJob job = ExportJob.builder()
                .requestedBy(requestedBy.getId())
                .reportType(reportType)
                .status(ExportJobStatus.PENDING)
                .build();
        ExportJob saved = exportJobRepository.save(job);
        generateReportAsync(saved.getId(), requestedBy);
        return saved;
    }

    @Async("reportTaskExecutor")
    public void generateReportAsync(Long jobId, User requestedBy) {
        ExportJob job = exportJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        try {
            job.setStatus(ExportJobStatus.PROCESSING);
            exportJobRepository.save(job);

            // Simulate report generation
            Thread.sleep(100);

            String filePath = "/exports/" + jobId + "_" + job.getReportType() + ".csv";
            job.setFilePath(filePath);
            job.setStatus(ExportJobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            exportJobRepository.save(job);

            notificationService.createInAppNotification(
                    requestedBy.getId(),
                    "REPORT_READY",
                    "Your " + job.getReportType() + " report is ready for download.",
                    "/hr/reports/download/" + jobId
            );

            log.info("Report job {} completed for user {}", jobId, requestedBy.getId());
        } catch (Exception e) {
            log.error("Report job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(ExportJobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            exportJobRepository.save(job);
        }
    }

    @Transactional(readOnly = true)
    public ExportJob getJobStatus(Long jobId) {
        return exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Export job not found: " + jobId));
    }
}
