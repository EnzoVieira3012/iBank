package com.ibank.card.event;

import com.ibank.card.model.TransactionType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class CardTransactionAuthorizedEvent extends ApplicationEvent {
    private final UUID transactionId;
    private final UUID cardId;
    private final UUID accountId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String authorizationCode;

    public CardTransactionAuthorizedEvent(Object source, UUID transactionId, UUID cardId, UUID accountId,
                                          TransactionType type, BigDecimal amount, String authorizationCode) {
        super(source);
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.authorizationCode = authorizationCode;
    }
}