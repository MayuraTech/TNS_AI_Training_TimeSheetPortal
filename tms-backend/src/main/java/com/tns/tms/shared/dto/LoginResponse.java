package com.tns.tms.shared.dto;

import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;

import java.util.Set;

public record LoginResponse(
    Long userId,
    String fullName,
    String email,
    Set<Role> roles,
    Role activeRole,
    String timezone,
    boolean forcePasswordChange
) {
    public static LoginResponse from(User user) {
        Role activeRole = user.getRoles().stream()
                .max(java.util.Comparator.comparingInt(Role::ordinal))
                .orElse(Role.EMPLOYEE);
        return new LoginResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRoles(),
                activeRole,
                user.getTimezone(),
                user.isForcePasswordChange()
        );
    }
}
