package com.ibank.antifraud.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class FraudCaseCreatedEvent extends ApplicationEvent {
    private final UUID caseId;
    private final String caseNumber;
    private final UUID customerId;
    private final UUID accountId;
    private final BigDecimal totalAmount;
    private final Integer alertCount;

    public FraudCaseCreatedEvent(Object source, UUID caseId, String caseNumber, UUID customerId,
                                 UUID accountId, BigDecimal totalAmount, Integer alertCount) {
        super(source);
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.customerId = customerId;
        this.accountId = accountId;
        this.totalAmount = totalAmount;
        this.alertCount = alertCount;
    }
}