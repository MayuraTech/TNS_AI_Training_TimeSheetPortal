package com.tns.tms.domain.timesheet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.admin.Project;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "timesheet_entries", schema = "TMS4")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "roles", "passwordHash"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Project project;

    @Column(name = "manager_id_at_submission")
    private Long managerIdAtSubmission;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;

    @Column(name = "task_description", length = 500)
    private String taskDescription;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal hours;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "overtime_justification", length = 300)
    private String overtimeJustification;

    @Column(name = "is_auto_approved", nullable = false)
    @Builder.Default
    private boolean autoApproved = false;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant submittedAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public boolean isEditable() {
        return status == ApprovalStatus.PENDING || status == ApprovalStatus.CLARIFICATION_REQUESTED;
    }

    public boolean isDeletable() {
        return status == ApprovalStatus.PENDING;
    }
}
