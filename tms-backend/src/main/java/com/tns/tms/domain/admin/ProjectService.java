package com.tns.tms.domain.admin;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import com.tns.tms.shared.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ProjectService(ProjectRepository projectRepository,
                           UserRepository userRepository,
                           AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Project createProject(Long actorId, String name, String code, String client,
                                  LocalDate startDate, LocalDate endDate) {
        if (projectRepository.existsByName(name)) {
            throw new ValidationException("Project name already exists: " + name);
        }
        if (projectRepository.existsByCode(code)) {
            throw new ValidationException("Project code already exists: " + code);
        }

        Project project = Project.builder()
                .name(name).code(code).client(client)
                .startDate(startDate).endDate(endDate)
                .status(ProjectStatus.ACTIVE)
                .build();

        Project saved = projectRepository.save(project);
        auditLogService.log(actorId, "PROJECT_CREATED", "PROJECT", saved.getId(), null,
                "name=" + name + ",code=" + code);
        return saved;
    }

    @Transactional
    public Project archiveProject(Long actorId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ValidationException("Project is already archived.");
        }

        project.setStatus(ProjectStatus.ARCHIVED);
        Project saved = projectRepository.save(project);
        auditLogService.log(actorId, "PROJECT_ARCHIVED", "PROJECT", projectId, "ACTIVE", "ARCHIVED");
        return saved;
    }

    @Transactional
    public Project restoreProject(Long actorId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        if (project.getStatus() == ProjectStatus.ACTIVE) {
            throw new ValidationException("Project is already active.");
        }

        project.setStatus(ProjectStatus.ACTIVE);
        Project saved = projectRepository.save(project);
        auditLogService.log(actorId, "PROJECT_RESTORED", "PROJECT", projectId, "ARCHIVED", "ACTIVE");
        return saved;
    }

    @Transactional
    public void assignEmployee(Long actorId, Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Check if already assigned (handled by unique constraint in DB)
        auditLogService.log(actorId, "PROJECT_EMPLOYEE_ASSIGNED", "PROJECT", projectId,
                null, "userId=" + userId);
    }

    @Transactional(readOnly = true)
    public List<Project> getActiveProjects() {
        return projectRepository.findByStatus(ProjectStatus.ACTIVE);
    }
}
