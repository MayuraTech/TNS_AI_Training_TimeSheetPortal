package com.tns.tms.domain.admin;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock private SystemConfigRepository systemConfigRepository;
    @Mock private AuditLogService auditLogService;

    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        systemConfigService = new SystemConfigService(systemConfigRepository, auditLogService);
    }

    @Test
    void updateConfig_existingKey_updatesAndAudits() {
        SystemConfig config = SystemConfig.builder()
                .key("daily_hours_overtime_threshold").value("9.0").build();
        when(systemConfigRepository.findById("daily_hours_overtime_threshold"))
                .thenReturn(Optional.of(config));
        when(systemConfigRepository.save(any())).thenReturn(config);

        SystemConfig result = systemConfigService.updateConfig(99L, "daily_hours_overtime_threshold", "10.0");

        assertThat(result.getValue()).isEqualTo("10.0");
        verify(auditLogService).log(eq(99L), eq("CONFIG_UPDATED"), eq("SYSTEM_CONFIG"),
                isNull(), anyString(), anyString());
    }

    @Test
    void updateConfig_nonExistingKey_throwsResourceNotFoundException() {
        when(systemConfigRepository.findById("unknown_key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemConfigService.updateConfig(99L, "unknown_key", "value"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllConfig_returnsAllKeyValuePairs() {
        List<SystemConfig> configs = List.of(
                SystemConfig.builder().key("key1").value("val1").build(),
                SystemConfig.builder().key("key2").value("val2").build()
        );
        when(systemConfigRepository.findAll()).thenReturn(configs);

        Map<String, String> result = systemConfigService.getAllConfig();

        assertThat(result).hasSize(2);
        assertThat(result.get("key1")).isEqualTo("val1");
    }

    @Test
    void updateConfig_immediateEffect_noRestartRequired() {
        // Verify config changes take effect immediately (no restart needed)
        SystemConfig config = SystemConfig.builder()
                .key("reminder_schedule_time").value("17:00").build();
        when(systemConfigRepository.findById("reminder_schedule_time"))
                .thenReturn(Optional.of(config));
        when(systemConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SystemConfig result = systemConfigService.updateConfig(99L, "reminder_schedule_time", "09:00");

        // Value is updated immediately in the returned object
        assertThat(result.getValue()).isEqualTo("09:00");
    }
}
