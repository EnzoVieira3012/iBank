package com.ibank.antifraud.controller.dto;

import com.ibank.antifraud.model.FraudRuleAction;
import com.ibank.antifraud.model.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RiskAssessmentResponse {

    private UUID customerId;
    private UUID transactionId;
    private Integer score;
    private RiskLevel riskLevel;
    private FraudRuleAction recommendedAction;
    private List<TriggeredRule> triggeredRules;
    private FactorScores factorScores;
    private boolean requiresReview;
    private String modelVersion;

    @Data
    @Builder
    public static class TriggeredRule {
        private UUID ruleId;
        private String ruleName;
        private String ruleType;
        private FraudRuleAction action;
        private Integer scoreImpact;
    }

    @Data
    @Builder
    public static class FactorScores {
        private Integer velocity;
        private Integer amount;
        private Integer geography;
        private Integer device;
        private Integer behavioral;
        private Integer merchant;
        private Integer beneficiary;
        private Integer channel;
        private Integer timePattern;
    }
}