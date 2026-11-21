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
@Table(name = "risk_scores", indexes = {
    @Index(name = "idx_risk_scores_customer", columnList = "customer_id"),
    @Index(name = "idx_risk_scores_account", columnList = "account_id"),
    @Index(name = "idx_risk_scores_card", columnList = "card_id"),
    @Index(name = "idx_risk_scores_transaction", columnList = "transaction_id"),
    @Index(name = "idx_risk_scores_level", columnList = "risk_level"),
    @Index(name = "idx_risk_scores_calculated", columnList = "calculated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScore extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "factor_scores", columnDefinition = "jsonb")
    private String factorScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "triggered_rules", columnDefinition = "jsonb")
    private String triggeredRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "jsonb")
    private String context;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "model_version", length = 20)
    private String modelVersion;

    @Version
    private Long version;
}