package com.ibank.credit.model;

import com.ibank.core.model.BaseEntity;
import com.ibank.core.util.Money;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "loan_simulations", indexes = {
    @Index(name = "idx_loan_sim_account_id", columnList = "account_id"),
    @Index(name = "idx_loan_sim_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LoanSimulation extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", length = 20, nullable = false)
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "amortization_type", length = 20, nullable = false)
    private AmortizationType amortizationType;

    @AttributeOverride(name = "amount", column = @Column(name = "requested_amount", precision = 19, scale = 2, nullable = false))
    private Money requestedAmount;

    @Column(name = "interest_rate", precision = 7, scale = 4, nullable = false)
    private java.math.BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @AttributeOverride(name = "amount", column = @Column(name = "installment_amount", precision = 19, scale = 2, nullable = false))
    private Money installmentAmount;

    @AttributeOverride(name = "amount", column = @Column(name = "total_amount", precision = 19, scale = 2, nullable = false))
    private Money totalAmount;

    @AttributeOverride(name = "amount", column = @Column(name = "total_interest", precision = 19, scale = 2, nullable = false))
    private Money totalInterest;

    @AttributeOverride(name = "amount", column = @Column(name = "origination_fee", precision = 19, scale = 2))
    private Money originationFee;

    @AttributeOverride(name = "amount", column = @Column(name = "insurance_amount", precision = 19, scale = 2))
    private Money insuranceAmount;

    @AttributeOverride(name = "amount", column = @Column(name = "cet", precision = 7, scale = 4))
    private java.math.BigDecimal cet;

    @Column(name = "first_due_date")
    private java.time.LocalDate firstDueDate;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String metadata;
}