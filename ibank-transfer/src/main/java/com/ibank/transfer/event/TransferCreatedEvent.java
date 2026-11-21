package com.ibank.transfer.event;

import com.ibank.core.util.Money;
import com.ibank.transfer.model.TransferType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class TransferCreatedEvent extends ApplicationEvent {

    private final UUID transferId;
    private final String endToEndId;
    private final TransferType transferType;
    private final UUID payerAccountId;
    private final UUID payeeAccountId;
    private final Money amount;
    private final String idempotencyKey;

    public TransferCreatedEvent(UUID transferId, String endToEndId, TransferType transferType,
                                UUID payerAccountId, UUID payeeAccountId, Money amount, String idempotencyKey) {
        super(transferId);
        this.transferId = transferId;
        this.endToEndId = endToEndId;
        this.transferType = transferType;
        this.payerAccountId = payerAccountId;
        this.payeeAccountId = payeeAccountId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }
}