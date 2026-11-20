package com.ibank.core.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends BusinessException {

    private final BigDecimal availableBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(BigDecimal availableBalance, BigDecimal requestedAmount) {
        super("INSUFFICIENT_FUNDS",
            "Insufficient funds. Available: " + availableBalance + ", Requested: " + requestedAmount,
            org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
}