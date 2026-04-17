package com.tns.tms.domain.manager;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import com.tns.tms.shared.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ManagerAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(ManagerAssignmentService.class);

    private final ManagerAssignmentRepository managerAssignmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ManagerAssignmentService(ManagerAssignmentRepository managerAssignmentRepository,
                                     UserRepository userRepository,
                                     AuditLogService auditLogService) {
        this.managerAssignmentRepository = managerAssignmentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ManagerAssignment assignManager(Long actorId, Long employeeId, Long managerId) {
        if (employeeId.equals(managerId)) {
            throw new ValidationException("An employee cannot be their own manager.");
        }

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + managerId));

        // Check for circular assignment
        if (wouldCreateCircularAssignment(employeeId, managerId)) {
            throw new ValidationException(
                "Circular assignment detected: this would create a management cycle.");
        }

        // Close existing active assignment
        managerAssignmentRepository.findActiveByEmployeeId(employeeId).ifPresent(existing -> {
            existing.setEffectiveTo(Instant.now());
            managerAssignmentRepository.save(existing);
        });

        ManagerAssignment assignment = ManagerAssignment.builder()
                .employee(employee)
                .manager(manager)
                .build();

        ManagerAssignment saved = managerAssignmentRepository.save(assignment);
        auditLogService.log(actorId, "MANAGER_ASSIGNED", "MANAGER_ASSIGNMENT", saved.getId(),
                null, "employee=" + employeeId + ",manager=" + managerId);

        log.info("Manager {} assigned to employee {} by admin {}", managerId, employeeId, actorId);
        return saved;
    }

    /**
     * Detects circular assignments using DFS traversal.
     * Returns true if assigning managerId as manager of employeeId would create a cycle.
     */
    private boolean wouldCreateCircularAssignment(Long employeeId, Long managerId) {
        // Check if managerId is already a direct report of employeeId (or transitively)
        Set<Long> visited = new HashSet<>();
        return isReachable(managerId, employeeId, visited);
    }

    private boolean isReachable(Long startId, Long targetId, Set<Long> visited) {
        if (startId.equals(targetId)) return true;
        if (visited.contains(startId)) return false;
        visited.add(startId);

        List<Long> directReports = managerAssignmentRepository.findDirectReportIds(startId);
        for (Long reportId : directReports) {
            if (isReachable(reportId, targetId, visited)) return true;
        }
        return false;
    }
}
