package com.ibank.fee.exception;

import com.ibank.core.exception.BusinessException;

public class InterestRateNotFoundException extends BusinessException {
    public InterestRateNotFoundException(String message) {
        super("INTEREST_RATE_NOT_FOUND", message);
    }
}