package com.ibank.fee.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class InterestAccruedEvent extends ApplicationEvent {
    private final UUID accrualId;
    private final UUID accountId;
    private final UUID cardId;
    private final UUID loanId;
    private final BigDecimal principalBalance;
    private final BigDecimal rateApplied;
    private final Integer days;
    private final BigDecimal interestAmount;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;

    public InterestAccruedEvent(Object source, UUID accrualId, UUID accountId, UUID cardId, UUID loanId,
                                BigDecimal principalBalance, BigDecimal rateApplied, Integer days,
                                BigDecimal interestAmount, BigDecimal taxAmount, BigDecimal totalAmount) {
        super(source);
        this.accrualId = accrualId;
        this.accountId = accountId;
        this.cardId = cardId;
        this.loanId = loanId;
        this.principalBalance = principalBalance;
        this.rateApplied = rateApplied;
        this.days = days;
        this.interestAmount = interestAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
    }
}