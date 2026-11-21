package com.ibank.antifraud.exception;

import com.ibank.core.exception.BusinessException;

public class FraudRuleNotFoundException extends BusinessException {
    public FraudRuleNotFoundException(String message) {
        super("FRAUD_RULE_NOT_FOUND", message);
    }
}