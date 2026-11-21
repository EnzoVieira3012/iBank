package com.ibank.antifraud.controller.dto;

import com.ibank.antifraud.model.AlertSeverity;
import com.ibank.antifraud.model.AlertStatus;
import com.ibank.antifraud.model.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FraudAlertResponse {

    private UUID id;
    private UUID customerId;
    private UUID accountId;
    private UUID cardId;
    private UUID transactionId;
    private UUID ruleId;
    private AlertStatus status;
    private AlertSeverity severity;
    private String title;
    private String description;
    private Integer riskScore;
    private RiskLevel riskLevel;
    private String triggeredRules;
    private String transactionContext;
    private UUID assignedTo;
    private LocalDateTime assignedAt;
    private LocalDateTime reviewedAt;
    private UUID reviewedBy;
    private String resolution;
    private String falsePositiveReason;
    private UUID caseId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}