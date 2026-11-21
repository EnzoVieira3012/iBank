package com.ibank.account.event;

import com.ibank.core.util.Money;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class BalanceChangedEvent extends ApplicationEvent {

    private final UUID accountId;
    private final String accountNumber;
    private final Money oldBalance;
    private final Money newBalance;
    private final String operation;
    private final String description;

    public BalanceChangedEvent(UUID accountId, String accountNumber, Money oldBalance,
                               Money newBalance, String operation, String description) {
        super(accountId);
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.operation = operation;
        this.description = description;
    }
}