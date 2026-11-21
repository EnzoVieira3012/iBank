package com.ibank.limit.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class LimitValidationResult {

    private boolean allowed;
    private UUID customerId;
    private UUID accountId;
    private UUID cardId;
    private LimitType limitType;
    private BigDecimal limitValue;
    private BigDecimal currentUsage;
    private BigDecimal requestedAmount;
    private Long requestedCount;
    private BigDecimal remainingAmount;
    private Long remainingCount;
    private double usagePercentage;
    private boolean warningTriggered;
    private boolean blockTriggered;
    private String warningMessage;
    private String blockMessage;
    private List<TransactionBlock> activeBlocks;
}