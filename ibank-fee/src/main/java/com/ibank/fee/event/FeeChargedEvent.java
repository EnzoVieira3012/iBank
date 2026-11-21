package com.ibank.fee.event;

import com.ibank.fee.model.FeeType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class FeeChargedEvent extends ApplicationEvent {
    private final UUID feeChargeId;
    private final UUID accountId;
    private final UUID cardId;
    private final UUID loanId;
    private final UUID transferId;
    private final FeeType feeType;
    private final BigDecimal amount;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;

    public FeeChargedEvent(Object source, UUID feeChargeId, UUID accountId, UUID cardId, UUID loanId,
                           UUID transferId, FeeType feeType, BigDecimal amount, BigDecimal taxAmount,
                           BigDecimal totalAmount) {
        super(source);
        this.feeChargeId = feeChargeId;
        this.accountId = accountId;
        this.cardId = cardId;
        this.loanId = loanId;
        this.transferId = transferId;
        this.feeType = feeType;
        this.amount = amount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
    }
}