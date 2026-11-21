package com.ibank.antifraud.model;

import com.ibank.core.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_rules", indexes = {
    @Index(name = "idx_fraud_rules_type", columnList = "rule_type"),
    @Index(name = "idx_fraud_rules_active", columnList = "active"),
    @Index(name = "idx_fraud_rules_priority", columnList = "priority"),
    @Index(name = "idx_fraud_rules_dates", columnList = "valid_from,valid_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRule extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 30)
    private FraudRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private FraudRuleAction action;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb", nullable = false)
    private String conditions;

    @Column(name = "score_impact", nullable = false)
    private Integer scoreImpact;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "channel", length = 20)
    private String channel;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "customer_segment", length = 30)
    private String customerSegment;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Version
    private Long version;
}