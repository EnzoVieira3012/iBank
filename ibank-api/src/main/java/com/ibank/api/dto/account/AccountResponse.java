package com.ibank.api.dto.account;

import com.ibank.core.model.AccountStatus;
import com.ibank.core.model.AccountType;
import com.ibank.core.util.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private UUID id;
    private String accountNumber;
    private String branch;
    private AccountType accountType;
    private AccountStatus status;
    private String holderName;
    private String holderCpf;
    private String holderEmail;
    private String holderPhone;
    private java.time.LocalDate holderBirthDate;
    private Money balance;
    private Money availableBalance;
    private Money blockedBalance;
    private Money overdraftLimit;
    private Money overdraftUsed;
    private Money effectiveAvailableBalance;
    private Money dailyWithdrawalLimit;
    private Money dailyTransferLimit;
    private Money monthlyTransferLimit;
    private Money pixLimit;
    private Boolean pixEnabled;
    private Boolean debitCardEnabled;
    private Boolean creditCardEnabled;
    private BigDecimal interestRate;
    private Money maintenanceFee;
    private OffsetDateTime lastTransactionAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime blockedAt;
    private String blockedReason;
    private OffsetDateTime closedAt;
    private String closedReason;
}