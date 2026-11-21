package com.ibank.card.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
public class InvoiceGeneratedEvent extends ApplicationEvent {
    private final UUID invoiceId;
    private final UUID cardId;
    private final UUID accountId;
    private final LocalDate referenceMonth;
    private final BigDecimal totalAmount;
    private final LocalDate dueDate;

    public InvoiceGeneratedEvent(Object source, UUID invoiceId, UUID cardId, UUID accountId,
                                 LocalDate referenceMonth, BigDecimal totalAmount, LocalDate dueDate) {
        super(source);
        this.invoiceId = invoiceId;
        this.cardId = cardId;
        this.accountId = accountId;
        this.referenceMonth = referenceMonth;
        this.totalAmount = totalAmount;
        this.dueDate = dueDate;
    }
}