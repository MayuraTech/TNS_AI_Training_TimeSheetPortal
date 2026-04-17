package com.tns.tms.domain.admin;

import com.tns.tms.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Projects", description = "Project management")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ProjectService projectService;

    public ProjectController(ProjectRepository projectRepository, ProjectService projectService) {
        this.projectRepository = projectRepository;
        this.projectService = projectService;
    }

    /** All authenticated users can fetch active projects (for dropdowns) */
    @GetMapping("/api/projects/active")
    @Operation(summary = "Get active projects for current user")
    public ResponseEntity<List<Project>> getActiveProjects() {
        return ResponseEntity.ok(projectRepository.findByStatus(ProjectStatus.ACTIVE));
    }

    /** Admin-only full project list */
    @GetMapping("/api/admin/projects")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all projects (Admin)")
    public ResponseEntity<List<Project>> listProjects(
            @RequestParam(required = false) ProjectStatus status) {
        if (status != null) {
            return ResponseEntity.ok(projectRepository.findByStatus(status));
        }
        return ResponseEntity.ok(projectRepository.findAll());
    }

    @PostMapping("/api/admin/projects")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create project")
    public ResponseEntity<Project> createProject(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        LocalDate startDate = body.get("startDate") != null ? LocalDate.parse(body.get("startDate")) : null;
        LocalDate endDate = body.get("endDate") != null ? LocalDate.parse(body.get("endDate")) : null;
        Project project = projectService.createProject(
                currentUser.getId(),
                body.get("name"), body.get("code"), body.get("client"),
                startDate, endDate
        );
        return ResponseEntity.ok(project);
    }

    @PostMapping("/api/admin/projects/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archive project")
    public ResponseEntity<Project> archiveProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projectService.archiveProject(currentUser.getId(), id));
    }

    @PostMapping("/api/admin/projects/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Restore archived project")
    public ResponseEntity<Project> restoreProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projectService.restoreProject(currentUser.getId(), id));
    }
}
