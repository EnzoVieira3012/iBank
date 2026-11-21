package com.ibank.fee.controller.dto;

import com.ibank.fee.model.FeeType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class FeeChargeRequest {

    private UUID accountId;

    private UUID cardId;

    private UUID loanId;

    private UUID transferId;

    private UUID feeConfigId;

    @NotNull(message = "Fee type is required")
    private FeeType feeType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Charge date is required")
    private LocalDate chargeDate;

    private LocalDate dueDate;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private BigDecimal taxAmount;

    private String idempotencyKey;

    private String calculationDetails;

    private String metadata;
}