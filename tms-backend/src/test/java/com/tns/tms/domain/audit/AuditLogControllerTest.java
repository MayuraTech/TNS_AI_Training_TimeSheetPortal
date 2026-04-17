package com.tns.tms.domain.audit;

import com.tns.tms.config.TestSecurityConfig;
import com.tns.tms.config.WithMockTmsUser;
import com.tns.tms.domain.auth.JwtAuthFilter;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@Import(TestSecurityConfig.class)
class AuditLogControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean(name = "filterUserRepo") UserRepository filterUserRepo;
    @MockBean AuditLogRepository auditLogRepository;

    private User admin;
    private AuditLog sampleLog;

    @BeforeEach
    void setUp() {
        admin = WithMockTmsUser.admin(1L);
        sampleLog = AuditLog.builder()
                .id(1L).actorId(1L).actionType("LOGIN")
                .entityType("USER").entityId(1L)
                .createdAt(Instant.now()).build();
    }

    @Test
    void getAuditLog_noFilters_returns200WithPage() throws Exception {
        when(auditLogRepository.findWithFilters(isNull(), isNull(), isNull(),
                isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        mockMvc.perform(get("/api/admin/audit-log")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].actionType").value("LOGIN"));
    }

    @Test
    void getAuditLog_withActorFilter_returns200() throws Exception {
        when(auditLogRepository.findWithFilters(eq(1L), isNull(), isNull(),
                isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        mockMvc.perform(get("/api/admin/audit-log")
                        .param("actorId", "1")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actorId").value(1));
    }

    @Test
    void getAuditLog_withActionTypeFilter_returns200() throws Exception {
        when(auditLogRepository.findWithFilters(isNull(), eq("LOGIN"), isNull(),
                isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        mockMvc.perform(get("/api/admin/audit-log")
                        .param("actionType", "LOGIN")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void getAuditLog_withEntityTypeFilter_returns200() throws Exception {
        when(auditLogRepository.findWithFilters(isNull(), isNull(), eq("USER"),
                isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        mockMvc.perform(get("/api/admin/audit-log")
                        .param("entityType", "USER")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void getAuditLog_emptyResult_returns200EmptyPage() throws Exception {
        when(auditLogRepository.findWithFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/audit-log")
                        .with(WithMockTmsUser.as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }
}
