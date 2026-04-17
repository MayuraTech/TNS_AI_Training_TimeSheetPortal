package com.tns.tms.domain.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.tms.config.TestSecurityConfig;
import com.tns.tms.config.WithMockTmsUser;
import com.tns.tms.domain.auth.JwtAuthFilter;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@Import(TestSecurityConfig.class)
class ReportControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean(name = "filterUserRepo") UserRepository filterUserRepo;
    @MockBean ReportService reportService;

    private User hrUser;
    private ExportJob pendingJob;
    private ExportJob completedJob;

    @BeforeEach
    void setUp() {
        hrUser = WithMockTmsUser.hr(1L);
        pendingJob = ExportJob.builder()
                .id(1L).requestedBy(1L).reportType("WEEKLY_COMPLIANCE")
                .status(ExportJobStatus.PENDING).createdAt(Instant.now()).build();
        completedJob = ExportJob.builder()
                .id(1L).requestedBy(1L).reportType("WEEKLY_COMPLIANCE")
                .status(ExportJobStatus.COMPLETED).filePath("/exports/1.csv")
                .createdAt(Instant.now()).completedAt(Instant.now()).build();
    }

    @Test
    void generateReport_validRequest_returns200WithJob() throws Exception {
        when(reportService.createJob(any(), eq("WEEKLY_COMPLIANCE"))).thenReturn(pendingJob);

        mockMvc.perform(post("/api/hr/reports/generate")
                        .with(WithMockTmsUser.as(hrUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("reportType", "WEEKLY_COMPLIANCE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reportType").value("WEEKLY_COMPLIANCE"));
    }

    @Test
    void generateReport_monthlyHours_returns200() throws Exception {
        ExportJob job = ExportJob.builder()
                .id(2L).requestedBy(1L).reportType("MONTHLY_HOURS_SUMMARY")
                .status(ExportJobStatus.PENDING).createdAt(Instant.now()).build();
        when(reportService.createJob(any(), eq("MONTHLY_HOURS_SUMMARY"))).thenReturn(job);

        mockMvc.perform(post("/api/hr/reports/generate")
                        .with(WithMockTmsUser.as(hrUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("reportType", "MONTHLY_HOURS_SUMMARY"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportType").value("MONTHLY_HOURS_SUMMARY"));
    }

    @Test
    void getJobStatus_pendingJob_returns200() throws Exception {
        when(reportService.getJobStatus(1L)).thenReturn(pendingJob);

        mockMvc.perform(get("/api/hr/reports/exports/1/status")
                        .with(WithMockTmsUser.as(hrUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getJobStatus_completedJob_returns200WithFilePath() throws Exception {
        when(reportService.getJobStatus(1L)).thenReturn(completedJob);

        mockMvc.perform(get("/api/hr/reports/exports/1/status")
                        .with(WithMockTmsUser.as(hrUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.filePath").value("/exports/1.csv"));
    }

    @Test
    void getJobStatus_notFound_returns404() throws Exception {
        when(reportService.getJobStatus(999L))
                .thenThrow(new ResourceNotFoundException("Export job not found: 999"));

        mockMvc.perform(get("/api/hr/reports/exports/999/status")
                        .with(WithMockTmsUser.as(hrUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
