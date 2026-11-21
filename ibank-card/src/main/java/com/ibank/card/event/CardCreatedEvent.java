package com.ibank.card.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class CardCreatedEvent extends ApplicationEvent {
    private final UUID cardId;
    private final UUID accountId;
    private final String cardType;

    public CardCreatedEvent(Object source, UUID cardId, UUID accountId, String cardType) {
        super(source);
        this.cardId = cardId;
        this.accountId = accountId;
        this.cardType = cardType;
    }
}