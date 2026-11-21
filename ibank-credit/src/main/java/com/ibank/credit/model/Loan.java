package com.ibank.credit.model;

import com.ibank.core.model.BaseEntity;
import com.ibank.core.util.Money;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "loans", indexes = {
    @Index(name = "idx_loans_account_id", columnList = "account_id"),
    @Index(name = "idx_loans_status", columnList = "status"),
    @Index(name = "idx_loans_type", columnList = "loan_type"),
    @Index(name = "idx_loans_contract_number", columnList = "contract_number", unique = true),
    @Index(name = "idx_loans_disbursed_at", columnList = "disbursed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Loan extends BaseEntity {

    @Column(name = "contract_number", length = 30, nullable = false, unique = true)
    private String contractNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", length = 20, nullable = false)
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private LoanStatus status = LoanStatus.SIMULATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "amortization_type", length = 20, nullable = false)
    private AmortizationType amortizationType;

    @AttributeOverride(name = "amount", column = @Column(name = "principal_amount", precision = 19, scale = 2, nullable = false))
    private Money principalAmount;

    @AttributeOverride(name = "amount", column = @Column(name = "total_amount", precision = 19, scale = 2, nullable = false))
    private Money totalAmount;

    @AttributeOverride(name = "amount", column = @Column(name = "total_interest", precision = 19, scale = 2, nullable = false))
    private Money totalInterest;

    @AttributeOverride(name = "amount", column = @Column(name = "outstanding_balance", precision = 19, scale = 2, nullable = false))
    private Money outstandingBalance;

    @AttributeOverride(name = "amount", column = @Column(name = "overdue_amount", precision = 19, scale = 2, nullable = false))
    @Builder.Default
    private Money overdueAmount = Money.ZERO;

    @Column(name = "interest_rate", precision = 7, scale = 4, nullable = false)
    private java.math.BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "installment_amount", precision = 19, scale = 2)
    private Money installmentAmount;

    @Column(name = "first_due_date")
    private LocalDate firstDueDate;

    @Column(name = "last_due_date")
    private LocalDate lastDueDate;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "holder_name", length = 255)
    private String holderName;

    @Column(name = "holder_cpf", length = 18)
    private String holderCpf;

    @Column(name = "disbursed_at")
    private OffsetDateTime disbursedAt;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancelled_reason", length = 500)
    private String cancelledReason;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "collateral_type", length = 20)
    @Enumerated(EnumType.STRING)
    private CollateralType collateralType;

    @Column(name = "collateral_description", length = 500)
    private String collateralDescription;

    @AttributeOverride(name = "amount", column = @Column(name = "collateral_value", precision = 19, scale = 2))
    private Money collateralValue;

    @Column(name = "insurance_amount", precision = 19, scale = 2)
    private Money insuranceAmount;

    @Column(name = "origination_fee", precision = 19, scale = 2)
    private Money originationFee;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    public void disburse() {
        this.status = LoanStatus.DISBURSED;
        this.disbursedAt = OffsetDateTime.now();
    }

    public void activate() {
        this.status = LoanStatus.ACTIVE;
    }

    public void markOverdue(Money overdueAmount) {
        this.status = LoanStatus.OVERDUE;
        this.overdueAmount = this.overdueAmount.add(overdueAmount);
    }

    public void payInstallment(Money amount) {
        this.outstandingBalance = this.outstandingBalance.subtract(amount);
        if (this.overdueAmount.isPositive()) {
            Money paidOverdue = this.overdueAmount.min(amount);
            this.overdueAmount = this.overdueAmount.subtract(paidOverdue);
        }
        if (this.outstandingBalance.isZero()) {
            this.status = LoanStatus.SETTLED;
            this.settledAt = OffsetDateTime.now();
        }
    }

    public void renegotiate() {
        this.status = LoanStatus.RENEGOTIATED;
    }

    public boolean isActive() {
        return status == LoanStatus.ACTIVE || status == LoanStatus.OVERDUE || status == LoanStatus.RENEGOTIATED;
    }
}