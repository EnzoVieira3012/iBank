package com.ibank.fee.event;

import com.ibank.fee.model.FeeType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class FeePaidEvent extends ApplicationEvent {
    private final UUID feeChargeId;
    private final UUID accountId;
    private final FeeType feeType;
    private final BigDecimal paidAmount;
    private final UUID paymentTransactionId;

    public FeePaidEvent(Object source, UUID feeChargeId, UUID accountId, FeeType feeType,
                        BigDecimal paidAmount, UUID paymentTransactionId) {
        super(source);
        this.feeChargeId = feeChargeId;
        this.accountId = accountId;
        this.feeType = feeType;
        this.paidAmount = paidAmount;
        this.paymentTransactionId = paymentTransactionId;
    }
}