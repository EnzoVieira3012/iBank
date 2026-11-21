package com.ibank.account.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class AccountClosedEvent extends ApplicationEvent {

    private final UUID accountId;
    private final String accountNumber;
    private final String holderCpf;
    private final String reason;

    public AccountClosedEvent(UUID accountId, String accountNumber, String holderCpf, String reason) {
        super(accountId);
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.holderCpf = holderCpf;
        this.reason = reason;
    }
}