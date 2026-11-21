package com.ibank.fee.exception;

import com.ibank.core.exception.BusinessException;

public class FeeChargeNotFoundException extends BusinessException {
    public FeeChargeNotFoundException(String message) {
        super("FEE_CHARGE_NOT_FOUND", message);
    }
}