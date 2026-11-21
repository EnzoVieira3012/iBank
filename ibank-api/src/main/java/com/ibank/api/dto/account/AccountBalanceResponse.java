package com.ibank.api.dto.account;

import com.ibank.core.util.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {

    private UUID id;
    private String accountNumber;
    private Money balance;
    private Money availableBalance;
    private Money blockedBalance;
    private Money overdraftLimit;
    private Money overdraftUsed;
    private Money effectiveAvailableBalance;
}