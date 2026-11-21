package com.ibank.account.model;

import com.ibank.core.model.AccountStatus;
import com.ibank.core.model.AccountType;
import com.ibank.core.model.BaseEntity;
import com.ibank.core.util.Money;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_accounts_account_number", columnList = "account_number", unique = true),
    @Index(name = "idx_accounts_holder_cpf", columnList = "holder_cpf"),
    @Index(name = "idx_accounts_status", columnList = "status"),
    @Index(name = "idx_accounts_branch", columnList = "branch")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, exclude = {"balance"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Account extends BaseEntity {

    @Column(name = "account_number", length = 20, nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "branch", length = 10, nullable = false)
    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20, nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.PENDING;

    @Column(name = "holder_name", length = 255, nullable = false)
    private String holderName;

    @Column(name = "holder_cpf", length = 18, nullable = false)
    private String holderCpf;

    @Column(name = "holder_email", length = 255)
    private String holderEmail;

    @Column(name = "holder_phone", length = 20)
    private String holderPhone;

    @Column(name = "holder_birth_date")
    private java.time.LocalDate holderBirthDate;

    @AttributeOverride(name = "amount", column = @Column(name = "balance", precision = 19, scale = 2, nullable = false))
    private Money balance = Money.ZERO;

    @AttributeOverride(name = "amount", column = @Column(name = "available_balance", precision = 19, scale = 2, nullable = false))
    private Money availableBalance = Money.ZERO;

    @AttributeOverride(name = "amount", column = @Column(name = "blocked_balance", precision = 19, scale = 2, nullable = false))
    private Money blockedBalance = Money.ZERO;

    @AttributeOverride(name = "amount", column = @Column(name = "overdraft_limit", precision = 19, scale = 2, nullable = false))
    private Money overdraftLimit = Money.ZERO;

    @AttributeOverride(name = "amount", column = @Column(name = "overdraft_used", precision = 19, scale = 2, nullable = false))
    private Money overdraftUsed = Money.ZERO;

    @Column(name = "daily_withdrawal_limit", precision = 19, scale = 2)
    private Money dailyWithdrawalLimit;

    @Column(name = "daily_transfer_limit", precision = 19, scale = 2)
    private Money dailyTransferLimit;

    @Column(name = "monthly_transfer_limit", precision = 19, scale = 2)
    private Money monthlyTransferLimit;

    @Column(name = "pix_limit", precision = 19, scale = 2)
    private Money pixLimit;

    @Column(name = "is_pix_enabled", nullable = false)
    @Builder.Default
    private Boolean pixEnabled = true;

    @Column(name = "is_debit_card_enabled", nullable = false)
    @Builder.Default
    private Boolean debitCardEnabled = false;

    @Column(name = "is_credit_card_enabled", nullable = false)
    @Builder.Default
    private Boolean creditCardEnabled = false;

    @Column(name = "interest_rate", precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "maintenance_fee", precision = 19, scale = 2)
    private Money maintenanceFee;

    @Column(name = "last_transaction_at")
    private OffsetDateTime lastTransactionAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "closed_reason", length = 500)
    private String closedReason;

    @Column(name = "blocked_at")
    private OffsetDateTime blockedAt;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public void credit(Money amount) {
        this.balance = this.balance.add(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    public void debit(Money amount) {
        this.balance = this.balance.subtract(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
    }

    public void blockAmount(Money amount) {
        this.availableBalance = this.availableBalance.subtract(amount);
        this.blockedBalance = this.blockedBalance.add(amount);
    }

    public void releaseBlockedAmount(Money amount) {
        this.blockedBalance = this.blockedBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    public boolean canWithdraw(Money amount) {
        if (status != AccountStatus.ACTIVE) return false;
        Money maxAvailable = availableBalance.add(overdraftLimit.subtract(overdraftUsed));
        return maxAvailable.isGreaterThanOrEqual(amount);
    }

    public void useOverdraft(Money amount) {
        this.overdraftUsed = this.overdraftUsed.add(amount);
    }

    public void releaseOverdraft(Money amount) {
        this.overdraftUsed = this.overdraftUsed.subtract(amount);
    }

    public Money getEffectiveAvailableBalance() {
        if (accountType.isAllowsOverdraft()) {
            return availableBalance.add(overdraftLimit.subtract(overdraftUsed));
        }
        return availableBalance;
    }
}