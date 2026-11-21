package com.ibank.card.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class InvoicePaidEvent extends ApplicationEvent {
    private final UUID invoiceId;
    private final UUID cardId;
    private final UUID accountId;
    private final BigDecimal paidAmount;
    private final String paymentMethod;

    public InvoicePaidEvent(Object source, UUID invoiceId, UUID cardId, UUID accountId,
                            BigDecimal paidAmount, String paymentMethod) {
        super(source);
        this.invoiceId = invoiceId;
        this.cardId = cardId;
        this.accountId = accountId;
        this.paidAmount = paidAmount;
        this.paymentMethod = paymentMethod;
    }
}