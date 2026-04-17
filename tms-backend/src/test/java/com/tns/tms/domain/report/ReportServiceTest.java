package com.tns.tms.domain.report;

import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.timesheet.TimesheetEntryRepository;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ExportJobRepository exportJobRepository;
    @Mock private NotificationService notificationService;
    @Mock private TimesheetEntryRepository entryRepository;

    private ReportService reportService;
    private User hrUser;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(exportJobRepository, notificationService, entryRepository);
        hrUser = User.builder().id(1L).email("hr@example.com").fullName("HR User")
                .passwordHash("hash").status(UserStatus.ACTIVE).roles(Set.of(Role.HR)).build();
    }

    @Test
    void createJob_validRequest_createsJobWithPendingStatus() {
        when(exportJobRepository.save(any())).thenAnswer(inv -> {
            ExportJob job = inv.getArgument(0);
            job = ExportJob.builder()
                    .id(1L).requestedBy(job.getRequestedBy())
                    .reportType(job.getReportType()).status(ExportJobStatus.PENDING).build();
            return job;
        });
        when(exportJobRepository.findById(1L)).thenReturn(Optional.empty()); // async won't run

        ExportJob job = reportService.createJob(hrUser, "WEEKLY_COMPLIANCE");

        assertThat(job.getStatus()).isEqualTo(ExportJobStatus.PENDING);
        assertThat(job.getReportType()).isEqualTo("WEEKLY_COMPLIANCE");
        assertThat(job.getRequestedBy()).isEqualTo(1L);
    }

    @Test
    void getJobStatus_existingJob_returnsJob() {
        ExportJob job = ExportJob.builder()
                .id(1L).requestedBy(1L).reportType("WEEKLY_COMPLIANCE")
                .status(ExportJobStatus.COMPLETED).build();

        when(exportJobRepository.findById(1L)).thenReturn(Optional.of(job));

        ExportJob result = reportService.getJobStatus(1L);

        assertThat(result.getStatus()).isEqualTo(ExportJobStatus.COMPLETED);
    }

    @Test
    void generateReportAsync_completesSuccessfully() throws InterruptedException {
        ExportJob job = ExportJob.builder()
                .id(1L).requestedBy(1L).reportType("WEEKLY_COMPLIANCE")
                .status(ExportJobStatus.PENDING).build();

        when(exportJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(exportJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reportService.generateReportAsync(1L, hrUser);

        // Wait for async to complete
        Thread.sleep(500);

        assertThat(job.getStatus()).isEqualTo(ExportJobStatus.COMPLETED);
        assertThat(job.getFilePath()).isNotNull();
    }
}
