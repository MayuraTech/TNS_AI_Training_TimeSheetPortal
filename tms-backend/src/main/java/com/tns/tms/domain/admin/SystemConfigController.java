package com.tns.tms.domain.admin;

import com.tns.tms.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
@Tag(name = "System Config", description = "System configuration management")
@PreAuthorize("hasRole('ADMIN')")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    @Operation(summary = "Get all system configuration values")
    public ResponseEntity<Map<String, String>> getAllConfig() {
        return ResponseEntity.ok(systemConfigService.getAllConfig());
    }

    @PutMapping
    @Operation(summary = "Update multiple config values")
    public ResponseEntity<Map<String, String>> updateConfig(
            @RequestBody Map<String, String> updates,
            @AuthenticationPrincipal User currentUser) {
        systemConfigService.updateMultipleConfigs(currentUser.getId(), updates);
        return ResponseEntity.ok(systemConfigService.getAllConfig());
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update a single config value")
    public ResponseEntity<SystemConfig> updateSingleConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(systemConfigService.updateConfig(currentUser.getId(), key, body.get("value")));
    }
}
