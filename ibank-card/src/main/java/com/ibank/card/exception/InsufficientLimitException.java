package com.ibank.card.exception;

import com.ibank.core.exception.BusinessException;

public class InsufficientLimitException extends BusinessException {
    public InsufficientLimitException(String message) {
        super("INSUFFICIENT_LIMIT", message);
    }
}