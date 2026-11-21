package com.ibank.card.exception;

import com.ibank.core.exception.BusinessException;

public class CardNotFoundException extends BusinessException {
    public CardNotFoundException(String message) {
        super("CARD_NOT_FOUND", message);
    }
}