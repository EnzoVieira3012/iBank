package com.ibank.transfer.event;

import com.ibank.transfer.model.TransferType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class TransferCancelledEvent extends ApplicationEvent {

    private final UUID transferId;
    private final String endToEndId;
    private final TransferType transferType;
    private final UUID payerAccountId;
    private final String reason;

    public TransferCancelledEvent(UUID transferId, String endToEndId, TransferType transferType,
                                  UUID payerAccountId, String reason) {
        super(transferId);
        this.transferId = transferId;
        this.endToEndId = endToEndId;
        this.transferType = transferType;
        this.payerAccountId = payerAccountId;
        this.reason = reason;
    }
}