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
@Table(name = "reconciliation_rule", indexes = {
    @Index(name = "idx_recon_rule_job_type", columnList = "job_type"),
    @Index(name = "idx_recon_rule_active", columnList = "active"),
    @Index(name = "idx_recon_rule_priority", columnList = "priority")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReconciliationRule {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "match_keys", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> matchKeys;

    @Column(name = "compare_fields", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> compareFields;

    @Column(name = "tolerance_config", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> toleranceConfig;

    @Column(name = "break_type_mapping", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> breakTypeMapping;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;
}