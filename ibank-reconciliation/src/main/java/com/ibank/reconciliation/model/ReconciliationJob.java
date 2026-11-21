package com.ibank.reconciliation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_job", indexes = {
    @Index(name = "idx_recon_job_status", columnList = "status"),
    @Index(name = "idx_recon_job_type", columnList = "job_type"),
    @Index(name = "idx_recon_job_started", columnList = "started_at"),
    @Index(name = "idx_recon_job_external", columnList = "external_reference")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReconciliationJob {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "source_system", length = 100)
    private String sourceSystem;

    @Column(name = "target_system", length = 100)
    private String targetSystem;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    @Column(name = "total_records", nullable = false)
    @Builder.Default
    private Long totalRecords = 0L;

    @Column(name = "matched_records", nullable = false)
    @Builder.Default
    private Long matchedRecords = 0L;

    @Column(name = "mismatched_records", nullable = false)
    @Builder.Default
    private Long mismatchedRecords = 0L;

    @Column(name = "missing_in_source", nullable = false)
    @Builder.Default
    private Long missingInSource = 0L;

    @Column(name = "missing_in_target", nullable = false)
    @Builder.Default
    private Long missingInTarget = 0L;

    @Column(name = "breaks_count", nullable = false)
    @Builder.Default
    private Long breaksCount = 0L;

    @Column(name = "resolved_breaks", nullable = false)
    @Builder.Default
    private Long resolvedBreaks = 0L;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Version
    private Long version;

    public enum Status {
        PENDING, RUNNING, COMPLETED, COMPLETED_WITH_BREAKS, FAILED, CANCELLED
    }

    public enum JobType {
        DAILY_SETTLEMENT, INTRA_DAY, END_OF_DAY, ON_DEMAND, SCHEDULED
    }
}