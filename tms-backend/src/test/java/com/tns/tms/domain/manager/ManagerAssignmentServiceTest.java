package com.tns.tms.domain.manager;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.domain.user.UserStatus;
import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerAssignmentServiceTest {

    @Mock private ManagerAssignmentRepository managerAssignmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private ManagerAssignmentService managerAssignmentService;

    private User employee;
    private User manager;

    @BeforeEach
    void setUp() {
        managerAssignmentService = new ManagerAssignmentService(
                managerAssignmentRepository, userRepository, auditLogService);

        employee = User.builder().id(1L).email("emp@example.com").fullName("Employee")
                .passwordHash("h").status(UserStatus.ACTIVE).roles(Set.of(Role.EMPLOYEE)).build();

        manager = User.builder().id(2L).email("mgr@example.com").fullName("Manager")
                .passwordHash("h").status(UserStatus.ACTIVE).roles(Set.of(Role.MANAGER)).build();
    }

    @Test
    void assign_validAssignment_createsAssignment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(managerAssignmentRepository.findActiveByEmployeeId(1L)).thenReturn(Optional.empty());
        when(managerAssignmentRepository.findDirectReportIds(2L)).thenReturn(List.of());
        when(managerAssignmentRepository.save(any())).thenAnswer(inv -> {
            ManagerAssignment ma = inv.getArgument(0);
            ma = ManagerAssignment.builder().id(1L).employee(ma.getEmployee()).manager(ma.getManager()).build();
            return ma;
        });

        ManagerAssignment result = managerAssignmentService.assignManager(99L, 1L, 2L);

        assertThat(result.getEmployee().getId()).isEqualTo(1L);
        assertThat(result.getManager().getId()).isEqualTo(2L);
    }

    @Test
    void assign_circularDependency_throws() {
        // Manager 2 is already a direct report of Employee 1
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        // Employee 1 manages Manager 2 (so assigning Manager 2 as manager of Employee 1 would be circular)
        when(managerAssignmentRepository.findDirectReportIds(2L)).thenReturn(List.of(1L));

        assertThatThrownBy(() -> managerAssignmentService.assignManager(99L, 1L, 2L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Circular assignment");
    }

    @Test
    void assign_selfAssignment_throws() {
        assertThatThrownBy(() -> managerAssignmentService.assignManager(99L, 1L, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("own manager");
    }

    @Test
    void reassign_closesExistingAndCreatesNew() {
        User newManager = User.builder().id(3L).email("newmgr@example.com").fullName("New Manager")
                .passwordHash("h").status(UserStatus.ACTIVE).roles(Set.of(Role.MANAGER)).build();

        ManagerAssignment existingAssignment = ManagerAssignment.builder()
                .id(1L).employee(employee).manager(manager).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newManager));
        when(managerAssignmentRepository.findActiveByEmployeeId(1L)).thenReturn(Optional.of(existingAssignment));
        when(managerAssignmentRepository.findDirectReportIds(3L)).thenReturn(List.of());
        when(managerAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        managerAssignmentService.assignManager(99L, 1L, 3L);

        // Verify old assignment was closed
        assertThat(existingAssignment.getEffectiveTo()).isNotNull();
        verify(managerAssignmentRepository, times(2)).save(any()); // once for close, once for new
    }
}
