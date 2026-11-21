package com.ibank.card.controller.dto;

import com.ibank.card.model.TransactionStatus;
import com.ibank.card.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CardTransactionResponse {

    private UUID id;
    private UUID cardId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private String merchantCategoryCode;
    private String merchantCity;
    private String merchantCountry;
    private String authorizationCode;
    private String nsu;
    private String tid;
    private Integer installments;
    private Integer currentInstallment;
    private String description;
    private UUID originalTransactionId;
    private String reversalReason;
    private LocalDateTime processedAt;
    private LocalDateTime settledAt;
    private String idempotencyKey;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}