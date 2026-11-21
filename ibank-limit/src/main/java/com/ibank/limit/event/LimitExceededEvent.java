package com.ibank.limit.event;

import com.ibank.limit.model.LimitType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class LimitExceededEvent extends ApplicationEvent {
    private final UUID customerId;
    private final UUID accountId;
    private final UUID cardId;
    private final LimitType limitType;
    private final BigDecimal limitValue;
    private final BigDecimal currentUsage;
    private final BigDecimal requestedAmount;
    private final Long requestedCount;
    private final String scope;

    public LimitExceededEvent(Object source, UUID customerId, UUID accountId, UUID cardId,
                              LimitType limitType, BigDecimal limitValue, BigDecimal currentUsage,
                              BigDecimal requestedAmount, Long requestedCount, String scope) {
        super(source);
        this.customerId = customerId;
        this.accountId = accountId;
        this.cardId = cardId;
        this.limitType = limitType;
        this.limitValue = limitValue;
        this.currentUsage = currentUsage;
        this.requestedAmount = requestedAmount;
        this.requestedCount = requestedCount;
        this.scope = scope;
    }
}