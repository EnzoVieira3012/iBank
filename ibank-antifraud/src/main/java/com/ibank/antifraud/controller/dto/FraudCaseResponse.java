package com.ibank.antifraud.controller.dto;

import com.ibank.antifraud.model.CaseStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FraudCaseResponse {

    private UUID id;
    private String caseNumber;
    private UUID customerId;
    private UUID accountId;
    private UUID cardId;
    private CaseStatus status;
    private String title;
    private String description;
    private BigDecimal totalAmount;
    private String currency;
    private Integer transactionCount;
    private LocalDate firstTransactionDate;
    private LocalDate lastTransactionDate;
    private UUID assignedTo;
    private LocalDateTime assignedAt;
    private String investigatorNotes;
    private String resolution;
    private LocalDateTime resolvedAt;
    private UUID resolvedBy;
    private Boolean sarFiled;
    private LocalDateTime sarFiledAt;
    private String sarReference;
    private LocalDateTime escalatedAt;
    private UUID escalatedTo;
    private String alertIds;
    private String evidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}