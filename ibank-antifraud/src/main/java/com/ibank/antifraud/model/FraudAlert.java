package com.ibank.antifraud.model;

import com.ibank.core.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_alerts", indexes = {
    @Index(name = "idx_alerts_customer", columnList = "customer_id"),
    @Index(name = "idx_alerts_account", columnList = "account_id"),
    @Index(name = "idx_alerts_card", columnList = "card_id"),
    @Index(name = "idx_alerts_transaction", columnList = "transaction_id"),
    @Index(name = "idx_alerts_status", columnList = "status"),
    @Index(name = "idx_alerts_severity", columnList = "severity"),
    @Index(name = "idx_alerts_created", columnList = "created_at"),
    @Index(name = "idx_alerts_assigned", columnList = "assigned_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "triggered_rules", columnDefinition = "jsonb")
    private String triggeredRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transaction_context", columnDefinition = "jsonb")
    private String transactionContext;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "resolution", length = 500)
    private String resolution;

    @Column(name = "false_positive_reason", length = 500)
    private String falsePositiveReason;

    @Column(name = "case_id")
    private UUID caseId;

    @Version
    private Long version;
}