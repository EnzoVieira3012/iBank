package com.ibank.antifraud.exception;

import com.ibank.core.exception.BusinessException;

public class FraudAlertNotFoundException extends BusinessException {
    public FraudAlertNotFoundException(String message) {
        super("FRAUD_ALERT_NOT_FOUND", message);
    }
}