package com.ibank.card.exception;

import com.ibank.core.exception.BusinessException;

public class CardAlreadyExistsException extends BusinessException {
    public CardAlreadyExistsException(String message) {
        super("CARD_ALREADY_EXISTS", message);
    }
}