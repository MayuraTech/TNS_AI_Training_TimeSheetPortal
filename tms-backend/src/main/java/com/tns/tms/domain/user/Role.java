package com.tns.tms.domain.user;

/**
 * RBAC roles in the TMS system.
 * Every user has EMPLOYEE as baseline; other roles add capabilities on top.
 */
public enum Role {
    EMPLOYEE,
    MANAGER,
    HR,
    ADMIN
}
