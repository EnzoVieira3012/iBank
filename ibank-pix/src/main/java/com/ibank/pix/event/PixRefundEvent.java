package com.ibank.pix.event;

import com.ibank.core.util.Money;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class PixRefundEvent extends ApplicationEvent {

    private final UUID refundTransactionId;
    private final String refundEndToEndId;
    private final UUID originalTransactionId;
    private final String originalEndToEndId;
    private final UUID payerAccountId;
    private final UUID payeeAccountId;
    private final Money refundAmount;
    private final String reason;

    public PixRefundEvent(UUID refundTransactionId, String refundEndToEndId,
                          UUID originalTransactionId, String originalEndToEndId,
                          UUID payerAccountId, UUID payeeAccountId,
                          Money refundAmount, String reason) {
        super(refundTransactionId);
        this.refundTransactionId = refundTransactionId;
        this.refundEndToEndId = refundEndToEndId;
        this.originalTransactionId = originalTransactionId;
        this.originalEndToEndId = originalEndToEndId;
        this.payerAccountId = payerAccountId;
        this.payeeAccountId = payeeAccountId;
        this.refundAmount = refundAmount;
        this.reason = reason;
    }
}