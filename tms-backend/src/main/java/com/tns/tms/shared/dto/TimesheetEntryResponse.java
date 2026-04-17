package com.tns.tms.shared.dto;

import com.tns.tms.domain.timesheet.ApprovalStatus;
import com.tns.tms.domain.timesheet.TimesheetEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record TimesheetEntryResponse(
    Long id,
    Long userId,
    String userFullName,
    Long projectId,
    String projectName,
    String projectCode,
    Long managerIdAtSubmission,
    LocalDate date,
    String taskName,
    String taskDescription,
    BigDecimal hours,
    ApprovalStatus status,
    String overtimeJustification,
    boolean autoApproved,
    Instant submittedAt,
    Instant updatedAt
) {
    public static TimesheetEntryResponse from(TimesheetEntry e) {
        return new TimesheetEntryResponse(
            e.getId(),
            e.getUser() != null ? e.getUser().getId() : null,
            e.getUser() != null ? e.getUser().getFullName() : null,
            e.getProject() != null ? e.getProject().getId() : null,
            e.getProject() != null ? e.getProject().getName() : null,
            e.getProject() != null ? e.getProject().getCode() : null,
            e.getManagerIdAtSubmission(),
            e.getDate(),
            e.getTaskName(),
            e.getTaskDescription(),
            e.getHours(),
            e.getStatus(),
            e.getOvertimeJustification(),
            e.isAutoApproved(),
            e.getSubmittedAt(),
            e.getUpdatedAt()
        );
    }
}
