package com.ibank.card.controller.dto;

import com.ibank.card.model.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardTransactionRequest {

    @NotNull(message = "Card ID is required")
    private String cardId;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (ISO 4217)")
    private String currency;

    private String merchantName;

    private String merchantCategoryCode;

    private String merchantCity;

    private String merchantCountry;

    private Integer installments;

    private String description;

    private String idempotencyKey;

    private String metadata;
}