package com.tns.tms.domain.admin;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final AuditLogService auditLogService;

    public SystemConfigService(SystemConfigRepository systemConfigRepository,
                                AuditLogService auditLogService) {
        this.systemConfigRepository = systemConfigRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAllConfig() {
        return systemConfigRepository.findAll().stream()
                .collect(Collectors.toMap(SystemConfig::getKey, SystemConfig::getValue));
    }

    @Transactional(readOnly = true)
    public String getConfigValue(String key) {
        return systemConfigRepository.findById(key)
                .map(SystemConfig::getValue)
                .orElseThrow(() -> new ResourceNotFoundException("Config key not found: " + key));
    }

    @Transactional
    public SystemConfig updateConfig(Long actorId, String key, String newValue) {
        SystemConfig config = systemConfigRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("Config key not found: " + key));

        String oldValue = config.getValue();
        config.setValue(newValue);
        config.setUpdatedBy(actorId);
        config.setUpdatedAt(Instant.now());

        SystemConfig saved = systemConfigRepository.save(config);
        auditLogService.log(actorId, "CONFIG_UPDATED", "SYSTEM_CONFIG", null,
                key + "=" + oldValue, key + "=" + newValue);

        return saved;
    }

    @Transactional
    public void updateMultipleConfigs(Long actorId, Map<String, String> updates) {
        updates.forEach((key, value) -> updateConfig(actorId, key, value));
    }
}
