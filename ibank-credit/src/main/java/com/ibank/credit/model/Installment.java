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
@Table(name = "installments", indexes = {
    @Index(name = "idx_installments_loan_id", columnList = "loan_id"),
    @Index(name = "idx_installments_due_date", columnList = "due_date"),
    @Index(name = "idx_installments_status", columnList = "status"),
    @Index(name = "idx_installments_loan_status", columnList = "loan_id, status"),
    @Index(name = "idx_installments_paid_at", columnList = "paid_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Installment extends BaseEntity {

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private InstallmentStatus status = InstallmentStatus.PENDING;

    @AttributeOverride(name = "amount", column = @Column(name = "principal_amount", precision = 19, scale = 2, nullable = false))
    private Money principalAmount;

    @AttributeOverride(name = "amount", column = @Column(name = "interest_amount", precision = 19, scale = 2, nullable = false))
    private Money interestAmount;

    @AttributeOverride(name = "amount", column = @Column(name = "total_amount", precision = 19, scale = 2, nullable = false))
    private Money totalAmount;

    @AttributeOverride(name = "amount", column = @Column(name = "paid_principal", precision = 19, scale = 2, nullable = false))
    @Builder.Default
    private Money paidPrincipal = Money.ZERO;

    @AttributeOverride(name = "amount", column = @Column(name = "paid_interest", precision = 19, scale = 2, nullable = false))
    @Builder.Default
    private Money paidInterest = Money.ZERO;

    @AttributeOverride(name = "amount", column = @Column(name = "paid_total", precision = 19, scale = 2, nullable = false))
    @Builder.Default
    private Money paidTotal = Money.ZERO;

    @AttributeOverride(name = "amount", column = @Column(name = "late_fee", precision = 19, scale = 2, nullable = false))
    @Builder.Default
    private Money lateFee = Money.ZERO;

    @AttributeOverride(name = "amount", column = @Column(name = "penalty_interest", precision = 19, scale = 2, nullable = false))
    @Builder.Default
    private Money penaltyInterest = Money.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "renegotiated_at")
    private OffsetDateTime renegotiatedAt;

    @Column(name = "renegotiated_from_installment_id")
    private UUID renegotiatedFromInstallmentId;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    public Money getRemainingPrincipal() {
        return principalAmount.subtract(paidPrincipal);
    }

    public Money getRemainingInterest() {
        return interestAmount.subtract(paidInterest);
    }

    public Money getRemainingTotal() {
        return totalAmount.subtract(paidTotal);
    }

    public boolean isFullyPaid() {
        return paidTotal.isGreaterThanOrEqual(totalAmount);
    }

    public void markPaid(Money principal, Money interest, UUID transactionId) {
        this.paidPrincipal = this.paidPrincipal.add(principal);
        this.paidInterest = this.paidInterest.add(interest);
        this.paidTotal = this.paidTotal.add(principal.add(interest));
        this.status = isFullyPaid() ? InstallmentStatus.PAID : InstallmentStatus.PARTIALLY_PAID;
        this.paidAt = OffsetDateTime.now();
        this.transactionId = transactionId;
    }

    public void addLateFee(Money fee) {
        this.lateFee = this.lateFee.add(fee);
        this.totalAmount = this.totalAmount.add(fee);
    }

    public void addPenaltyInterest(Money interest) {
        this.penaltyInterest = this.penaltyInterest.add(interest);
        this.totalAmount = this.totalAmount.add(interest);
    }
}