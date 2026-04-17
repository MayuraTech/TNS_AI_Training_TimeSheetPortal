package com.tns.tms.domain.user;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.manager.ManagerAssignment;
import com.tns.tms.domain.manager.ManagerAssignmentRepository;
import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ManagerAssignmentRepository managerAssignmentRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        userService = new UserService(userRepository, managerAssignmentRepository,
                passwordEncoder, auditLogService, notificationService);
    }

    @Test
    void createUser_validData_createsUserAndSendsWelcomeEmail() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByEmployeeId("EMP001")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u = User.builder().id(1L).email(u.getEmail()).fullName(u.getFullName())
                    .passwordHash(u.getPasswordHash()).roles(u.getRoles())
                    .department(u.getDepartment()).employeeId(u.getEmployeeId())
                    .forcePasswordChange(true).build();
            return u;
        });

        User result = userService.createUser(99L, "New User", "new@example.com",
                Set.of(Role.EMPLOYEE), null, "Engineering", "EMP001");

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.isForcePasswordChange()).isTrue();
        verify(notificationService).sendWelcomeEmail(any(), anyString());
        verify(auditLogService).log(eq(99L), eq("USER_CREATED"), eq("USER"), any(), isNull(), anyString());
    }

    @Test
    void createUser_duplicateEmail_throwsValidationException() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(99L, "User", "existing@example.com",
                Set.of(Role.EMPLOYEE), null, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void deactivateUser_activeUser_setsInactive() {
        User user = User.builder().id(1L).email("u@e.com").fullName("User")
                .passwordHash("h").status(UserStatus.ACTIVE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.deactivateUser(99L, 1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(auditLogService).log(eq(99L), eq("USER_DEACTIVATED"), eq("USER"), eq(1L), anyString(), anyString());
    }

    @Test
    void deactivateUser_alreadyInactive_throwsValidationException() {
        User user = User.builder().id(1L).email("u@e.com").fullName("User")
                .passwordHash("h").status(UserStatus.INACTIVE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deactivateUser(99L, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already inactive");
    }

    @Test
    void reactivateUser_inactiveUser_setsActive() {
        User user = User.builder().id(1L).email("u@e.com").fullName("User")
                .passwordHash("h").status(UserStatus.INACTIVE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.reactivateUser(99L, 1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
