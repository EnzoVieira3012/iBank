package com.ibank.card.exception;

import com.ibank.core.exception.BusinessException;

public class InvalidCardStatusException extends BusinessException {
    public InvalidCardStatusException(String message) {
        super("INVALID_CARD_STATUS", message);
    }
}