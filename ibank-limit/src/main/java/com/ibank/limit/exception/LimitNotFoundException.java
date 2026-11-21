package com.ibank.limit.exception;

import com.ibank.core.exception.BusinessException;

public class LimitNotFoundException extends BusinessException {
    public LimitNotFoundException(String message) {
        super("LIMIT_NOT_FOUND", message);
    }
}