package com.ibank.antifraud.exception;

import com.ibank.core.exception.BusinessException;

public class FraudCaseNotFoundException extends BusinessException {
    public FraudCaseNotFoundException(String message) {
        super("FRAUD_CASE_NOT_FOUND", message);
    }
}