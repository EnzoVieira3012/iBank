package com.ibank.pix.model;

import com.ibank.core.model.BaseEntity;
import com.ibank.core.util.Money;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pix_transactions", indexes = {
    @Index(name = "idx_pix_tx_payer_account", columnList = "payer_account_id"),
    @Index(name = "idx_pix_tx_payee_account", columnList = "payee_account_id"),
    @Index(name = "idx_pix_tx_payer_key", columnList = "payer_key_id"),
    @Index(name = "idx_pix_tx_payee_key", columnList = "payee_key_id"),
    @Index(name = "idx_pix_tx_status", columnList = "status"),
    @Index(name = "idx_pix_tx_end_to_end_id", columnList = "end_to_end_id", unique = true),
    @Index(name = "idx_pix_tx_created_at", columnList = "created_at"),
    @Index(name = "idx_pix_tx_refund_id", columnList = "refund_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PixTransaction extends BaseEntity {

    @Column(name = "end_to_end_id", length = 50, nullable = false, unique = true)
    private String endToEndId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private PixTransactionStatus status = PixTransactionStatus.PENDING;

    @AttributeOverride(name = "amount", column = @Column(name = "amount", precision = 19, scale = 2, nullable = false))
    private Money amount;

    @Column(name = "description", length = 140)
    private String description;

    // Payer info
    @Column(name = "payer_account_id")
    private UUID payerAccountId;

    @Column(name = "payer_name", length = 255)
    private String payerName;

    @Column(name = "payer_cpf", length = 18)
    private String payerCpf;

    @Column(name = "payer_key_id")
    private UUID payerKeyId;

    @Column(name = "payer_key_value", length = 100)
    private String payerKeyValue;

    @Column(name = "payer_key_type", length = 20)
    private String payerKeyType;

    @Column(name = "payer_branch", length = 10)
    private String payerBranch;

    @Column(name = "payer_account_number", length = 20)
    private String payerAccountNumber;

    // Payee info
    @Column(name = "payee_account_id")
    private UUID payeeAccountId;

    @Column(name = "payee_name", length = 255)
    private String payeeName;

    @Column(name = "payee_cpf", length = 18)
    private String payeeCpf;

    @Column(name = "payee_key_id")
    private UUID payeeKeyId;

    @Column(name = "payee_key_value", length = 100)
    private String payeeKeyValue;

    @Column(name = "payee_key_type", length = 20)
    private String payeeKeyType;

    @Column(name = "payee_branch", length = 10)
    private String payeeBranch;

    @Column(name = "payee_account_number", length = 20)
    private String payeeAccountNumber;

    // Refund
    @Column(name = "refund_id")
    private UUID refundId;

    @Column(name = "refund_end_to_end_id", length = 50)
    private String refundEndToEndId;

    @AttributeOverride(name = "amount", column = @Column(name = "refunded_amount", precision = 19, scale = 2))
    private Money refundedAmount;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    // Processing
    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "dict_response", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String dictResponse;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    public boolean isFinal() {
        return status.isFinal();
    }

    public boolean canRefund() {
        return status.canRefund() && (refundedAmount == null || refundedAmount.isLessThan(amount));
    }

    public Money getRemainingRefundableAmount() {
        if (refundedAmount == null) return amount;
        return amount.subtract(refundedAmount);
    }
}