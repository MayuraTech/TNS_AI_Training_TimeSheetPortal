package com.tns.tms.domain.user;

import com.tns.tms.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "User Management", description = "Admin user management")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "List users with search/filter")
    public ResponseEntity<Page<User>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String department,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userRepository.findWithFilters(search, status, department, pageable));
    }

    @PostMapping
    @Operation(summary = "Create user")
    public ResponseEntity<User> createUser(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        @SuppressWarnings("unchecked")
        Set<Role> roles = ((java.util.List<String>) body.get("roles")).stream()
                .map(Role::valueOf).collect(Collectors.toSet());

        User created = userService.createUser(
                currentUser.getId(),
                (String) body.get("fullName"),
                (String) body.get("email"),
                roles,
                body.get("managerId") != null ? Long.valueOf(body.get("managerId").toString()) : null,
                (String) body.get("department"),
                (String) body.get("employeeId")
        );
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        @SuppressWarnings("unchecked")
        java.util.Set<com.tns.tms.domain.user.Role> roles = null;
        if (body.get("roles") != null) {
            roles = ((java.util.List<String>) body.get("roles")).stream()
                    .map(com.tns.tms.domain.user.Role::valueOf)
                    .collect(java.util.stream.Collectors.toSet());
        }
        User updated = userService.updateUser(
                currentUser.getId(), id,
                (String) body.get("fullName"),
                (String) body.get("email"),
                (String) body.get("department"),
                (String) body.get("employeeId"),
                roles
        );
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        userService.deactivateUser(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate user")
    public ResponseEntity<Void> reactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        userService.reactivateUser(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Trigger password reset for user")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        userService.resetPassword(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
