package com.ibank.reconciliation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_break", indexes = {
    @Index(name = "idx_recon_break_job", columnList = "job_id"),
    @Index(name = "idx_recon_break_status", columnList = "status"),
    @Index(name = "idx_recon_break_type", columnList = "break_type"),
    @Index(name = "idx_recon_break_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_recon_break_created", columnList = "created_at"),
    @Index(name = "idx_recon_break_assignee", columnList = "assignee_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReconciliationBreak {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ReconciliationJob job;

    @Column(name = "break_type", nullable = false, length = 50)
    private String breakType;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(name = "source_reference", length = 100)
    private String sourceReference;

    @Column(name = "target_reference", length = 100)
    private String targetReference;

    @Column(name = "source_amount", precision = 19, scale = 4)
    private BigDecimal sourceAmount;

    @Column(name = "target_amount", precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(name = "difference_amount", precision = 19, scale = 4)
    private BigDecimal differenceAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "priority", length = 20)
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(name = "assignee_id", length = 100)
    private String assigneeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_data", columnDefinition = "jsonb")
    private Map<String, Object> sourceData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_data", columnDefinition = "jsonb")
    private Map<String, Object> targetData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resolution_data", columnDefinition = "jsonb")
    private Map<String, Object> resolutionData;

    @Column(name = "resolution_notes", length = 2000)
    private String resolutionNotes;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "due_date")
    private Instant dueDate;

    @Version
    private Long version;

    public enum BreakType {
        AMOUNT_MISMATCH,
        MISSING_IN_SOURCE,
        MISSING_IN_TARGET,
        DUPLICATE_IN_SOURCE,
        DUPLICATE_IN_TARGET,
        TIMING_DIFFERENCE,
        REFERENCE_MISMATCH,
        STATUS_MISMATCH,
        CURRENCY_MISMATCH,
        FEE_MISMATCH
    }

    public enum Status {
        OPEN, IN_PROGRESS, RESOLVED, ESCALATED, IGNORED, FALSE_POSITIVE
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}