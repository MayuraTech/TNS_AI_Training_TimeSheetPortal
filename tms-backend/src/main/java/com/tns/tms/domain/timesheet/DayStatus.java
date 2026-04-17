package com.tns.tms.domain.timesheet;

/**
 * Computed day-level status derived from task-level ApprovalStatus values.
 * Never stored — always derived.
 */
public enum DayStatus {
    APPROVED,
    REJECTED,
    CLARIFICATION_REQUESTED,
    PENDING,
    NO_ENTRIES
}
