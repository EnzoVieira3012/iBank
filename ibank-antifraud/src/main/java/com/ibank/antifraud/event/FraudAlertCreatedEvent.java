package com.ibank.antifraud.event;

import com.ibank.antifraud.model.AlertSeverity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class FraudAlertCreatedEvent extends ApplicationEvent {
    private final UUID alertId;
    private final UUID customerId;
    private final UUID accountId;
    private final UUID cardId;
    private final UUID transactionId;
    private final UUID ruleId;
    private final AlertSeverity severity;
    private final Integer riskScore;
    private final String title;

    public FraudAlertCreatedEvent(Object source, UUID alertId, UUID customerId, UUID accountId, UUID cardId,
                                  UUID transactionId, UUID ruleId, AlertSeverity severity,
                                  Integer riskScore, String title) {
        super(source);
        this.alertId = alertId;
        this.customerId = customerId;
        this.accountId = accountId;
        this.cardId = cardId;
        this.transactionId = transactionId;
        this.ruleId = ruleId;
        this.severity = severity;
        this.riskScore = riskScore;
        this.title = title;
    }
}