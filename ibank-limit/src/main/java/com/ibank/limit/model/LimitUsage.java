package com.ibank.limit.model;

import com.ibank.core.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "limit_usages", indexes = {
    @Index(name = "idx_usage_customer", columnList = "customer_id"),
    @Index(name = "idx_usage_account", columnList = "account_id"),
    @Index(name = "idx_usage_card", columnList = "card_id"),
    @Index(name = "idx_usage_type", columnList = "limit_type"),
    @Index(name = "idx_usage_period", columnList = "period_start,period_end"),
    @Index(name = "idx_usage_updated", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LimitUsage extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "card_id")
    private UUID cardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false, length = 40)
    private LimitType limitType;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "count", nullable = false)
    private Long count;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;

    @Version
    private Long version;
}