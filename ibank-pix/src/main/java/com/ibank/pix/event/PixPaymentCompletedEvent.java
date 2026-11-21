package com.ibank.pix.event;

import com.ibank.core.util.Money;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class PixPaymentCompletedEvent extends ApplicationEvent {

    private final UUID transactionId;
    private final String endToEndId;
    private final UUID payerAccountId;
    private final UUID payeeAccountId;
    private final Money amount;
    private final UUID payerKeyId;
    private final UUID payeeKeyId;

    public PixPaymentCompletedEvent(UUID transactionId, String endToEndId,
                                    UUID payerAccountId, UUID payeeAccountId,
                                    Money amount, UUID payerKeyId, UUID payeeKeyId) {
        super(transactionId);
        this.transactionId = transactionId;
        this.endToEndId = endToEndId;
        this.payerAccountId = payerAccountId;
        this.payeeAccountId = payeeAccountId;
        this.amount = amount;
        this.payerKeyId = payerKeyId;
        this.payeeKeyId = payeeKeyId;
    }
}