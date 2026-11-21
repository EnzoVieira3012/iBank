package com.ibank.card.exception;

import com.ibank.core.exception.BusinessException;

public class TransactionNotFoundException extends BusinessException {
    public TransactionNotFoundException(String message) {
        super("TRANSACTION_NOT_FOUND", message);
    }
}