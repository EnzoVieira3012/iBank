package com.ibank.api.dto.account;

import com.ibank.core.model.AccountStatus;
import com.ibank.core.model.AccountType;
import com.ibank.core.util.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummaryResponse {

    private UUID id;
    private String accountNumber;
    private String branch;
    private AccountType accountType;
    private AccountStatus status;
    private String holderName;
    private String holderCpf;
    private Money balance;
    private Money availableBalance;
    private Money overdraftLimit;
    private Money overdraftUsed;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastTransactionAt;
}