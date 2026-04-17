package com.tns.tms.domain.timesheet;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotEmpty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DayStatusComputerTest {

    private DayStatusComputer computer;

    @BeforeEach
    void setUp() {
        computer = new DayStatusComputer();
    }

    @Test
    void compute_emptyList_returnsNoEntries() {
        assertThat(computer.compute(Collections.emptyList())).isEqualTo(DayStatus.NO_ENTRIES);
    }

    @Test
    void compute_nullList_returnsNoEntries() {
        assertThat(computer.compute(null)).isEqualTo(DayStatus.NO_ENTRIES);
    }

    @Test
    void compute_allApproved_returnsApproved() {
        assertThat(computer.compute(List.of(ApprovalStatus.APPROVED, ApprovalStatus.APPROVED)))
                .isEqualTo(DayStatus.APPROVED);
    }

    @Test
    void compute_allAutoApproved_returnsApproved() {
        assertThat(computer.compute(List.of(ApprovalStatus.AUTO_APPROVED, ApprovalStatus.AUTO_APPROVED)))
                .isEqualTo(DayStatus.APPROVED);
    }

    @Test
    void compute_mixedApprovedAndAutoApproved_returnsApproved() {
        assertThat(computer.compute(List.of(ApprovalStatus.APPROVED, ApprovalStatus.AUTO_APPROVED)))
                .isEqualTo(DayStatus.APPROVED);
    }

    @Test
    void compute_anyRejected_returnsRejected() {
        assertThat(computer.compute(List.of(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED)))
                .isEqualTo(DayStatus.REJECTED);
    }

    @Test
    void compute_rejectedTakesPrecedenceOverClarification() {
        assertThat(computer.compute(List.of(
                ApprovalStatus.CLARIFICATION_REQUESTED, ApprovalStatus.REJECTED)))
                .isEqualTo(DayStatus.REJECTED);
    }

    @Test
    void compute_clarificationNoRejected_returnsClarification() {
        assertThat(computer.compute(List.of(
                ApprovalStatus.APPROVED, ApprovalStatus.CLARIFICATION_REQUESTED)))
                .isEqualTo(DayStatus.CLARIFICATION_REQUESTED);
    }

    @Test
    void compute_pendingOnly_returnsPending() {
        assertThat(computer.compute(List.of(ApprovalStatus.PENDING)))
                .isEqualTo(DayStatus.PENDING);
    }

    @Test
    void compute_mixedPendingAndApproved_returnsPending() {
        assertThat(computer.compute(List.of(ApprovalStatus.APPROVED, ApprovalStatus.PENDING)))
                .isEqualTo(DayStatus.PENDING);
    }

    @Test
    void compute_singleApproved_returnsApproved() {
        assertThat(computer.compute(List.of(ApprovalStatus.APPROVED)))
                .isEqualTo(DayStatus.APPROVED);
    }

    // Property-based tests using jqwik
    @Property
    void anyRejectedAlwaysResultsInRejected(
            @ForAll @NotEmpty List<ApprovalStatus> statuses) {
        DayStatusComputer c = new DayStatusComputer();
        // If we add a REJECTED to any non-empty list, result must be REJECTED
        List<ApprovalStatus> withRejected = new java.util.ArrayList<>(statuses);
        withRejected.add(ApprovalStatus.REJECTED);
        assertThat(c.compute(withRejected)).isEqualTo(DayStatus.REJECTED);
    }

    @Property
    void allApprovedOrAutoApprovedAlwaysResultsInApproved(
            @ForAll @NotEmpty @From("approvedStatuses") List<ApprovalStatus> statuses) {
        DayStatusComputer c = new DayStatusComputer();
        assertThat(c.compute(statuses)).isEqualTo(DayStatus.APPROVED);
    }

    @Provide
    Arbitrary<List<ApprovalStatus>> approvedStatuses() {
        return Arbitraries.of(ApprovalStatus.APPROVED, ApprovalStatus.AUTO_APPROVED)
                .list().ofMinSize(1);
    }
}
