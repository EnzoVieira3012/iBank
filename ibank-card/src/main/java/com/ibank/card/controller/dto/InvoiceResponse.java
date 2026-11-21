package com.ibank.card.controller.dto;

import com.ibank.card.model.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class InvoiceResponse {

    private UUID id;
    private UUID cardId;
    private LocalDate referenceMonth;
    private LocalDate openingDate;
    private LocalDate closingDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal minimumPayment;
    private BigDecimal interestAmount;
    private BigDecimal feeAmount;
    private BigDecimal previousBalance;
    private LocalDateTime paidAt;
    private String paymentMethod;
    private UUID paymentTransactionId;
    private String details;
    private List<InvoiceInstallmentResponse> installments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}