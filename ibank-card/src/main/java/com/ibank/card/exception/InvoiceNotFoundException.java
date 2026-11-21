package com.ibank.card.exception;

import com.ibank.core.exception.BusinessException;

public class InvoiceNotFoundException extends BusinessException {
    public InvoiceNotFoundException(String message) {
        super("INVOICE_NOT_FOUND", message);
    }
}