package com.tns.tms.domain.manager;

import com.tns.tms.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/manager-assignments")
@Tag(name = "Manager Assignments", description = "Manager-employee assignment management")
@PreAuthorize("hasRole('ADMIN')")
public class ManagerAssignmentController {

    private final ManagerAssignmentRepository managerAssignmentRepository;
    private final ManagerAssignmentService managerAssignmentService;

    public ManagerAssignmentController(ManagerAssignmentRepository managerAssignmentRepository,
                                        ManagerAssignmentService managerAssignmentService) {
        this.managerAssignmentRepository = managerAssignmentRepository;
        this.managerAssignmentService = managerAssignmentService;
    }

    @GetMapping
    @Operation(summary = "List all manager assignments")
    public ResponseEntity<List<Map<String, Object>>> listAssignments() {
        List<ManagerAssignment> assignments = managerAssignmentRepository.findAll();
        List<Map<String, Object>> result = assignments.stream().map(a -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", a.getId());
            m.put("employeeId",    a.getEmployee() != null ? a.getEmployee().getId() : null);
            m.put("employeeName",  a.getEmployee() != null ? a.getEmployee().getFullName() : null);
            m.put("employeeEmail", a.getEmployee() != null ? a.getEmployee().getEmail() : null);
            m.put("managerId",     a.getManager() != null ? a.getManager().getId() : null);
            m.put("managerName",   a.getManager() != null ? a.getManager().getFullName() : null);
            m.put("managerEmail",  a.getManager() != null ? a.getManager().getEmail() : null);
            m.put("effectiveFrom", a.getEffectiveFrom());
            m.put("effectiveTo",   a.getEffectiveTo());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "Create or update manager assignment")
    public ResponseEntity<Map<String, Object>> assignManager(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Long employeeId = Long.valueOf(body.get("employeeId").toString());
        Long managerId  = Long.valueOf(body.get("managerId").toString());

        ManagerAssignment assignment = managerAssignmentService.assignManager(
                currentUser.getId(), employeeId, managerId);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", assignment.getId());
        result.put("employeeId", employeeId);
        result.put("managerId",  managerId);
        result.put("effectiveFrom", assignment.getEffectiveFrom());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/org-chart")
    @Operation(summary = "Get org chart data")
    public ResponseEntity<List<Map<String, Object>>> getOrgChart() {
        List<ManagerAssignment> active = managerAssignmentRepository.findAll()
                .stream().filter(ManagerAssignment::isActive).toList();

        List<Map<String, Object>> result = active.stream().map(a -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("employeeId",   a.getEmployee() != null ? a.getEmployee().getId() : null);
            m.put("managerId",    a.getManager() != null ? a.getManager().getId() : null);
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }
}
