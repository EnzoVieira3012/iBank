package com.ibank.limit.controller.dto;

import com.ibank.limit.model.LimitType;
import com.ibank.limit.model.TransactionBlock;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class LimitValidationResponse {

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