package com.tns.tms.domain.timesheet;

import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Stateless utility that computes Day_Status from a collection of task-level ApprovalStatus values.
 *
 * Precedence rules:
 * 1. No entries → NO_ENTRIES
 * 2. ALL APPROVED or AUTO_APPROVED → APPROVED
 * 3. ANY REJECTED → REJECTED (regardless of other statuses)
 * 4. ANY CLARIFICATION_REQUESTED (and none REJECTED) → CLARIFICATION_REQUESTED
 * 5. Otherwise → PENDING
 */
@Component
public class DayStatusComputer {

    public DayStatus compute(Collection<ApprovalStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return DayStatus.NO_ENTRIES;
        }

        boolean hasRejected = false;
        boolean hasClarification = false;
        boolean hasPending = false;
        boolean allApproved = true;

        for (ApprovalStatus status : statuses) {
            switch (status) {
                case REJECTED -> {
                    hasRejected = true;
                    allApproved = false;
                }
                case CLARIFICATION_REQUESTED -> {
                    hasClarification = true;
                    allApproved = false;
                }
                case PENDING -> {
                    hasPending = true;
                    allApproved = false;
                }
                case APPROVED, AUTO_APPROVED -> {
                    // counts as approved
                }
            }
        }

        if (hasRejected) return DayStatus.REJECTED;
        if (hasClarification) return DayStatus.CLARIFICATION_REQUESTED;
        if (allApproved) return DayStatus.APPROVED;
        return DayStatus.PENDING;
    }

    public DayStatus computeFromEntries(Collection<TimesheetEntry> entries) {
        if (entries == null || entries.isEmpty()) return DayStatus.NO_ENTRIES;
        return compute(entries.stream().map(TimesheetEntry::getStatus).toList());
    }
}
