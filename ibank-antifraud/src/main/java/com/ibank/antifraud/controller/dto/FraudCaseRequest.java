package com.ibank.antifraud.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class FraudCaseRequest {

    @NotNull(message = "Customer ID is required")
    private String customerId;

    private String accountId;

    private String cardId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private BigDecimal totalAmount;

    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;

    private Integer transactionCount;

    private String alertIds;
}