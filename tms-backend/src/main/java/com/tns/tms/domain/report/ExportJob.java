package com.tns.tms.domain.report;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "export_jobs", schema = "TMS4")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ExportJobStatus status = ExportJobStatus.PENDING;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
