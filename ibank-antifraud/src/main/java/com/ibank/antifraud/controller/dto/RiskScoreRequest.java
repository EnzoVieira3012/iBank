package com.ibank.antifraud.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class RiskScoreRequest {

    @NotNull(message = "Customer ID is required")
    private String customerId;

    private String accountId;

    private String cardId;

    private String transactionId;

    private String context;

    private String channel;

    private String productCode;
}