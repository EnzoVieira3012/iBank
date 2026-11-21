package com.ibank.card.controller.dto;

import com.ibank.card.model.InstallmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InvoiceInstallmentResponse {

    private UUID id;
    private UUID invoiceId;
    private Integer installmentNumber;
    private Integer totalInstallments;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private InstallmentStatus status;
    private LocalDateTime paidAt;
    private UUID paymentTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}