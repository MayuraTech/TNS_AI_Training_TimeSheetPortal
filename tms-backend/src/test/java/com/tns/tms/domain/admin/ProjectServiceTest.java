package com.tns.tms.domain.admin;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, userRepository, auditLogService);
    }

    @Test
    void createProject_validData_createsProject() {
        when(projectRepository.existsByName("New Project")).thenReturn(false);
        when(projectRepository.existsByCode("NP001")).thenReturn(false);
        when(projectRepository.save(any())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p = Project.builder().id(1L).name(p.getName()).code(p.getCode())
                    .status(ProjectStatus.ACTIVE).build();
            return p;
        });

        Project result = projectService.createProject(99L, "New Project", "NP001", "Client", null, null);

        assertThat(result.getName()).isEqualTo("New Project");
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void createProject_duplicateName_throwsValidationException() {
        when(projectRepository.existsByName("Existing")).thenReturn(true);

        assertThatThrownBy(() -> projectService.createProject(99L, "Existing", "EX001", null, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("name already exists");
    }

    @Test
    void archiveProject_activeProject_setsArchived() {
        Project project = Project.builder().id(1L).name("P").code("P1")
                .status(ProjectStatus.ACTIVE).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenReturn(project);

        Project result = projectService.archiveProject(99L, 1L);

        assertThat(result.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void archiveProject_alreadyArchived_throwsValidationException() {
        Project project = Project.builder().id(1L).name("P").code("P1")
                .status(ProjectStatus.ARCHIVED).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.archiveProject(99L, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already archived");
    }

    @Test
    void restoreProject_archivedProject_setsActive() {
        Project project = Project.builder().id(1L).name("P").code("P1")
                .status(ProjectStatus.ARCHIVED).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenReturn(project);

        Project result = projectService.restoreProject(99L, 1L);

        assertThat(result.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }
}
