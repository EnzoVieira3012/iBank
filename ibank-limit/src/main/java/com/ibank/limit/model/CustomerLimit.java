package com.ibank.limit.model;

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
@Table(name = "customer_limits", indexes = {
    @Index(name = "idx_limits_customer", columnList = "customer_id"),
    @Index(name = "idx_limits_account", columnList = "account_id"),
    @Index(name = "idx_limits_card", columnList = "card_id"),
    @Index(name = "idx_limits_type", columnList = "limit_type"),
    @Index(name = "idx_limits_scope", columnList = "scope"),
    @Index(name = "idx_limits_active", columnList = "active"),
    @Index(name = "idx_limits_dates", columnList = "valid_from,valid_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerLimit extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "card_id")
    private UUID cardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false, length = 40)
    private LimitType limitType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private LimitScope scope;

    @Column(name = "limit_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal limitValue;

    @Column(name = "current_usage", precision = 19, scale = 2)
    private BigDecimal currentUsage;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "warning_threshold", precision = 5, scale = 2)
    private BigDecimal warningThreshold;

    @Column(name = "block_on_exceed")
    private Boolean blockOnExceed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    private String conditions;

    @Column(name = "description", length = 500)
    private String description;

    @Version
    private Long version;
}