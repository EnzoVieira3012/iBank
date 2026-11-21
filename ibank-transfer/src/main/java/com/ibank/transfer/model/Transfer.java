package com.ibank.transfer.model;

import com.ibank.core.model.BaseEntity;
import com.ibank.core.util.Money;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfers", indexes = {
    @Index(name = "idx_transfers_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_transfers_payer_account", columnList = "payer_account_id"),
    @Index(name = "idx_transfers_payee_account", columnList = "payee_account_id"),
    @Index(name = "idx_transfers_status", columnList = "status"),
    @Index(name = "idx_transfers_type", columnList = "transfer_type"),
    @Index(name = "idx_transfers_scheduled_at", columnList = "scheduled_at"),
    @Index(name = "idx_transfers_end_to_end_id", columnList = "end_to_end_id", unique = true),
    @Index(name = "idx_transfers_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Transfer extends BaseEntity {

    @Column(name = "idempotency_key", length = 100, nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "end_to_end_id", length = 50, nullable = false, unique = true)
    private String endToEndId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", length = 20, nullable = false)
    private TransferType transferType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private TransferStatus status = TransferStatus.PENDING;

    @AttributeOverride(name = "amount", column = @Column(name = "amount", precision = 19, scale = 2, nullable = false))
    private Money amount;

    @Column(name = "description", length = 140)
    private String description;

    // Payer info
    @Column(name = "payer_account_id", nullable = false)
    private UUID payerAccountId;

    @Column(name = "payer_name", length = 255)
    private String payerName;

    @Column(name = "payer_cpf", length = 18)
    private String payerCpf;

    @Column(name = "payer_branch", length = 10)
    private String payerBranch;

    @Column(name = "payer_account_number", length = 20)
    private String payerAccountNumber;

    @Column(name = "payer_bank_ispb", length = 8)
    private String payerBankIspb;

    // Payee info
    @Column(name = "payee_account_id")
    private UUID payeeAccountId;

    @Column(name = "payee_name", length = 255, nullable = false)
    private String payeeName;

    @Column(name = "payee_cpf", length = 18)
    private String payeeCpf;

    @Column(name = "payee_branch", length = 10)
    private String payeeBranch;

    @Column(name = "payee_account_number", length = 20)
    private String payeeAccountNumber;

    @Column(name = "payee_bank_ispb", length = 8)
    private String payeeBankIspb;

    @Column(name = "payee_bank_name", length = 100)
    private String payeeBankName;

    // Scheduling
    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancelled_reason", length = 500)
    private String cancelledReason;

    // Fee
    @AttributeOverride(name = "amount", column = @Column(name = "fee_amount", precision = 19, scale = 2))
    private Money feeAmount;

    @Column(name = "fee_charged_at")
    private OffsetDateTime feeChargedAt;

    // External reference (for TED/DOC)
    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    public boolean isInternal() {
        return transferType == TransferType.INTERNAL;
    }

    public boolean isInterbank() {
        return transferType == TransferType.TED || transferType == TransferType.DOC;
    }

    public boolean isScheduled() {
        return transferType == TransferType.SCHEDULED || scheduledAt != null;
    }

    public boolean canBeCancelled() {
        return status.canCancel();
    }

    public boolean isFinal() {
        return status.isFinal();
    }
}