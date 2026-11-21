package com.ibank.fee.model;

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
@Table(name = "fee_charges", indexes = {
    @Index(name = "idx_fee_charge_account", columnList = "account_id"),
    @Index(name = "idx_fee_charge_card", columnList = "card_id"),
    @Index(name = "idx_fee_charge_loan", columnList = "loan_id"),
    @Index(name = "idx_fee_charge_transfer", columnList = "transfer_id"),
    @Index(name = "idx_fee_charge_config", columnList = "fee_config_id"),
    @Index(name = "idx_fee_charge_status", columnList = "status"),
    @Index(name = "idx_fee_charge_date", columnList = "charge_date"),
    @Index(name = "idx_fee_charge_due_date", columnList = "due_date"),
    @Index(name = "idx_fee_charge_idempotency", columnList = "idempotency_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeCharge extends BaseEntity {

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "loan_id")
    private UUID loanId;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "fee_config_id")
    private UUID feeConfigId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 30)
    private FeeType feeType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "charge_date", nullable = false)
    private LocalDate chargeDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeeChargeStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_transaction_id")
    private UUID paymentTransactionId;

    @Column(name = "idempotency_key", length = 36)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "calculation_details", columnDefinition = "jsonb")
    private String calculationDetails;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Version
    private Long version;
}