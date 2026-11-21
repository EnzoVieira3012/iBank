package com.ibank.card.model;

import com.ibank.core.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_transactions", indexes = {
    @Index(name = "idx_card_trans_card_id", columnList = "card_id"),
    @Index(name = "idx_card_trans_status", columnList = "status"),
    @Index(name = "idx_card_trans_type", columnList = "type"),
    @Index(name = "idx_card_trans_created_at", columnList = "created_at"),
    @Index(name = "idx_card_trans_merchant", columnList = "merchant_name"),
    @Index(name = "idx_card_trans_authorization_code", columnList = "authorization_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardTransaction extends BaseEntity {

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "merchant_name", length = 200)
    private String merchantName;

    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    @Column(name = "merchant_city", length = 100)
    private String merchantCity;

    @Column(name = "merchant_country", length = 2)
    private String merchantCountry;

    @Column(name = "authorization_code", length = 20)
    private String authorizationCode;

    @Column(name = "nsu", length = 20)
    private String nsu;

    @Column(name = "tid", length = 20)
    private String tid;

    @Column(name = "installments")
    private Integer installments;

    @Column(name = "current_installment")
    private Integer currentInstallment;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "original_transaction_id")
    private UUID originalTransactionId;

    @Column(name = "reversal_reason", length = 255)
    private String reversalReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "idempotency_key", unique = true, length = 36)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Version
    private Long version;
}