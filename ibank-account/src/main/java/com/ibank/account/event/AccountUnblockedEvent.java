package com.ibank.account.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class AccountUnblockedEvent extends ApplicationEvent {

    private final UUID accountId;
    private final String accountNumber;
    private final String holderCpf;

    public AccountUnblockedEvent(UUID accountId, String accountNumber, String holderCpf) {
        super(accountId);
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.holderCpf = holderCpf;
    }
}