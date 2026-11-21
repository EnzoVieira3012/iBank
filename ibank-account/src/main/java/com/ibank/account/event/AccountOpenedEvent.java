package com.ibank.account.event;

import com.ibank.core.model.AccountType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class AccountOpenedEvent extends ApplicationEvent {

    private final UUID accountId;
    private final String accountNumber;
    private final String holderCpf;
    private final AccountType accountType;
    private final String idempotencyKey;

    public AccountOpenedEvent(UUID accountId, String accountNumber, String holderCpf,
                              AccountType accountType, String idempotencyKey) {
        super(accountId);
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.holderCpf = holderCpf;
        this.accountType = accountType;
        this.idempotencyKey = idempotencyKey;
    }
}