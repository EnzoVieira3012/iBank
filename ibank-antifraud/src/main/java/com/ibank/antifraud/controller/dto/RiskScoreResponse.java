package com.ibank.antifraud.controller.dto;

import com.ibank.antifraud.model.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RiskScoreResponse {

    private UUID id;
    private UUID customerId;
    private UUID accountId;
    private UUID cardId;
    private UUID transactionId;
    private Integer score;
    private RiskLevel riskLevel;
    private String factorScores;
    private String triggeredRules;
    private String context;
    private LocalDateTime calculatedAt;
    private String modelVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}