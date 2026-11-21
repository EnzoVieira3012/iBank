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
@Table(name = "interest_accruals", indexes = {
    @Index(name = "idx_interest_accrual_account", columnList = "account_id"),
    @Index(name = "idx_interest_accrual_card", columnList = "card_id"),
    @Index(name = "idx_interest_accrual_loan", columnList = "loan_id"),
    @Index(name = "idx_interest_accrual_date", columnList = "accrual_date"),
    @Index(name = "idx_interest_accrual_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestAccrual extends BaseEntity {

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "loan_id")
    private UUID loanId;

    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    @Column(name = "principal_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalBalance;

    @Column(name = "rate_applied", nullable = false, precision = 15, scale = 8)
    private BigDecimal rateApplied;

    @Column(name = "days", nullable = false)
    private Integer days;

    @Column(name = "interest_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal interestAmount;

    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InterestAccrualStatus status;

    @Column(name = "capitalized_at")
    private LocalDateTime capitalizedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "calculation_details", columnDefinition = "jsonb")
    private String calculationDetails;

    @Version
    private Long version;
}