package com.ibank.fee.exception;

import com.ibank.core.exception.BusinessException;

public class FeeConfigNotFoundException extends BusinessException {
    public FeeConfigNotFoundException(String message) {
        super("FEE_CONFIG_NOT_FOUND", message);
    }
}