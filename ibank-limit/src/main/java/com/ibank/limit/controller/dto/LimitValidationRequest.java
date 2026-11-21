package com.ibank.limit.controller.dto;

import com.ibank.limit.model.LimitType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class LimitValidationRequest {

    @NotNull(message = "Customer ID is required")
    private String customerId;

    private String accountId;

    private String cardId;

    @NotNull(message = "Limit type is required")
    private LimitType limitType;

    private BigDecimal requestedAmount;

    private Long requestedCount;

    private String channel;

    private String transactionType;

    private String ipAddress;

    private String deviceFingerprint;
}