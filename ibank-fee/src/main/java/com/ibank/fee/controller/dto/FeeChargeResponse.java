package com.ibank.fee.controller.dto;

import com.ibank.fee.model.FeeChargeStatus;
import com.ibank.fee.model.FeeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FeeChargeResponse {

    private UUID id;
    private UUID accountId;
    private UUID cardId;
    private UUID loanId;
    private UUID transferId;
    private UUID feeConfigId;
    private FeeType feeType;
    private String description;
    private LocalDate chargeDate;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private FeeChargeStatus status;
    private LocalDateTime paidAt;
    private UUID paymentTransactionId;
    private String idempotencyKey;
    private String calculationDetails;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}