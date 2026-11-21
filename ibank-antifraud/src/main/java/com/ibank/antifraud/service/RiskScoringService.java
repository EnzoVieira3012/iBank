package com.ibank.antifraud.service;

import com.ibank.antifraud.controller.dto.RiskAssessmentResponse;
import com.ibank.antifraud.controller.dto.RiskScoreRequest;
import com.ibank.antifraud.event.FraudAlertCreatedEvent;
import com.ibank.antifraud.mapper.FraudAlertMapper;
import com.ibank.antifraud.mapper.RiskScoreMapper;
import com.ibank.antifraud.model.*;
import com.ibank.antifraud.repository.FraudAlertRepository;
import com.ibank.antifraud.repository.FraudRuleRepository;
import com.ibank.antifraud.repository.RiskScoreRepository;
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskScoringService {

    private final FraudRuleRepository fraudRuleRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final DeviceFingerprintService deviceFingerprintService;
    private final FraudAlertMapper fraudAlertMapper;
    private final RiskScoreMapper riskScoreMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final int BASE_SCORE = 0;
    private static final int MAX_SCORE = 1000;
    private static final int ALERT_THRESHOLD = 300;
    private static final int HIGH_RISK_THRESHOLD = 500;
    private static final int CRITICAL_THRESHOLD = 700;

    @Transactional
    public RiskAssessmentResponse assessRisk(RiskScoreRequest request) {
        UUID customerId = UUID.fromString(request.getCustomerId());
        UUID accountId = request.getAccountId() != null ? UUID.fromString(request.getAccountId()) : null;
        UUID cardId = request.getCardId() != null ? UUID.fromString(request.getCardId()) : null;
        UUID transactionId = request.getTransactionId() != null ? UUID.fromString(request.getTransactionId()) : null;

        List<FraudRule> applicableRules = fraudRuleRepository.findApplicableRules(
                LocalDate.now(), request.getChannel(), request.getProductCode(), null);

        RiskAssessmentResponse.FactorScores factorScores = calculateFactorScores(request, applicableRules);
        List<RiskAssessmentResponse.TriggeredRule> triggeredRules = new ArrayList<>();
        int totalScore = BASE_SCORE;

        for (FraudRule rule : applicableRules) {
            if (evaluateRule(rule, request)) {
                int impact = rule.getScoreImpact() != null ? rule.getScoreImpact() : 0;
                totalScore += impact;

                triggeredRules.add(RiskAssessmentResponse.TriggeredRule.builder()
                        .ruleId(rule.getId())
                        .ruleName(rule.getName())
                        .ruleType(rule.getRuleType().name())
                        .action(rule.getAction())
                        .scoreImpact(impact)
                        .build());
            }
        }

        totalScore = Math.max(0, Math.min(MAX_SCORE, totalScore));
        RiskLevel riskLevel = determineRiskLevel(totalScore);
        FraudRuleAction recommendedAction = determineAction(riskLevel, triggeredRules);

        if (totalScore >= ALERT_THRESHOLD) {
            createAlert(customerId, accountId, cardId, transactionId, totalScore, riskLevel,
                    triggeredRules, request.getContext());
        }

        RiskScore riskScore = RiskScore.builder()
                .customerId(customerId)
                .accountId(accountId)
                .cardId(cardId)
                .transactionId(transactionId)
                .score(totalScore)
                .riskLevel(riskLevel)
                .factorScores(toJson(factorScores))
                .triggeredRules(toJson(triggeredRules))
                .context(request.getContext())
                .calculatedAt(LocalDateTime.now())
                .modelVersion("1.0")
                .build();

        riskScoreRepository.save(riskScore);

        return RiskAssessmentResponse.builder()
                .customerId(customerId)
                .transactionId(transactionId)
                .score(totalScore)
                .riskLevel(riskLevel)
                .recommendedAction(recommendedAction)
                .triggeredRules(triggeredRules)
                .factorScores(factorScores)
                .requiresReview(totalScore >= HIGH_RISK_THRESHOLD)
                .modelVersion("1.0")
                .build();
    }

    private RiskAssessmentResponse.FactorScores calculateFactorScores(RiskScoreRequest request, List<FraudRule> rules) {
        RiskAssessmentResponse.FactorScores.FactorScoresBuilder builder = RiskAssessmentResponse.FactorScores.builder();

        Map<FraudRuleType, Integer> typeScores = new EnumMap<>(FraudRuleType.class);
        for (FraudRule rule : rules) {
            if (evaluateRule(rule, request)) {
                int impact = rule.getScoreImpact() != null ? rule.getScoreImpact() : 0;
                typeScores.merge(rule.getRuleType(), impact, Integer::sum);
            }
        }

        builder.velocity(typeScores.getOrDefault(FraudRuleType.VELOCITY, 0)
                + typeScores.getOrDefault(FraudRuleType.VELOCITY_PIX, 0)
                + typeScores.getOrDefault(FraudRuleType.VELOCITY_TRANSFER, 0)
                + typeScores.getOrDefault(FraudRuleType.VELOCITY_CARD, 0));
        builder.amount(typeScores.getOrDefault(FraudRuleType.AMOUNT_THRESHOLD, 0)
                + typeScores.getOrDefault(FraudRuleType.ROUND_AMOUNT, 0)
                + typeScores.getOrDefault(FraudRuleType.STRUCTURING, 0));
        builder.geography(typeScores.getOrDefault(FraudRuleType.GEOGRAPHY, 0)
                + typeScores.getOrDefault(FraudRuleType.HIGH_RISK_COUNTRY, 0));
        builder.device(typeScores.getOrDefault(FraudRuleType.DEVICE_FINGERPRINT, 0)
                + typeScores.getOrDefault(FraudRuleType.NEW_DEVICE, 0));
        builder.behavioral(typeScores.getOrDefault(FraudRuleType.TIME_PATTERN, 0)
                + typeScores.getOrDefault(FraudRuleType.BEHAVIORAL_DEVIATION, 0));
        builder.merchant(typeScores.getOrDefault(FraudRuleType.MERCHANT_CATEGORY, 0)
                + typeScores.getOrDefault(FraudRuleType.HIGH_RISK_MCC, 0));
        builder.beneficiary(typeScores.getOrDefault(FraudRuleType.BENEFICIARY_PATTERN, 0)
                + typeScores.getOrDefault(FraudRuleType.NEW_BENEFICIARY, 0));
        builder.channel(typeScores.getOrDefault(FraudRuleType.CHANNEL_ANOMALY, 0));
        builder.timePattern(typeScores.getOrDefault(FraudRuleType.TIME_PATTERN, 0));

        return builder.build();
    }

    private boolean evaluateRule(FraudRule rule, RiskScoreRequest request) {
        // Simplified rule evaluation - in production use a rule engine like Drools or Easy Rules
        // For now, return true for demonstration
        return true;
    }

    private RiskLevel determineRiskLevel(int score) {
        if (score >= CRITICAL_THRESHOLD) return RiskLevel.CRITICAL;
        if (score >= HIGH_RISK_THRESHOLD) return RiskLevel.VERY_HIGH;
        if (score >= 400) return RiskLevel.HIGH;
        if (score >= 250) return RiskLevel.MEDIUM;
        if (score >= 100) return RiskLevel.LOW;
        return RiskLevel.VERY_LOW;
    }

    private FraudRuleAction determineAction(RiskLevel riskLevel, List<RiskAssessmentResponse.TriggeredRule> triggeredRules) {
        if (riskLevel == RiskLevel.CRITICAL) return FraudRuleAction.BLOCK;

        for (RiskAssessmentResponse.TriggeredRule tr : triggeredRules) {
            if (tr.getAction() == FraudRuleAction.BLOCK) return FraudRuleAction.BLOCK;
            if (tr.getAction() == FraudRuleAction.CHALLENGE) return FraudRuleAction.CHALLENGE;
            if (tr.getAction() == FraudRuleAction.REQUIRE_2FA) return FraudRuleAction.REQUIRE_2FA;
        }

        if (riskLevel == RiskLevel.VERY_HIGH) return FraudRuleAction.CHALLENGE;
        if (riskLevel == RiskLevel.HIGH) return FraudRuleAction.REVIEW;
        if (riskLevel == RiskLevel.MEDIUM) return FraudRuleAction.ALERT;

        return FraudRuleAction.ALLOW;
    }

    private void createAlert(UUID customerId, UUID accountId, UUID cardId, UUID transactionId,
                             int score, RiskLevel riskLevel,
                             List<RiskAssessmentResponse.TriggeredRule> triggeredRules,
                             String context) {
        AlertSeverity severity = determineSeverity(riskLevel);

        FraudAlert alert = FraudAlert.builder()
                .customerId(customerId)
                .accountId(accountId)
                .cardId(cardId)
                .transactionId(transactionId)
                .ruleId(triggeredRules.isEmpty() ? null : triggeredRules.get(0).getRuleId())
                .status(AlertStatus.OPEN)
                .severity(severity)
                .title(generateAlertTitle(riskLevel, triggeredRules))
                .description(generateAlertDescription(riskLevel, triggeredRules))
                .riskScore(score)
                .riskLevel(riskLevel)
                .triggeredRules(toJson(triggeredRules))
                .transactionContext(context)
                .build();

        FraudAlert saved = fraudAlertRepository.save(alert);

        eventPublisher.publishEvent(new FraudAlertCreatedEvent(
                this, saved.getId(), customerId, accountId, cardId, transactionId,
                alert.getRuleId(), severity, score, alert.getTitle()
        ));

        log.info("Fraud alert created: {} for customer {} with score {}", saved.getId(), customerId, score);
    }

    private AlertSeverity determineSeverity(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case CRITICAL -> AlertSeverity.CRITICAL;
            case VERY_HIGH, HIGH -> AlertSeverity.HIGH;
            case MEDIUM -> AlertSeverity.MEDIUM;
            default -> AlertSeverity.LOW;
        };
    }

    private String generateAlertTitle(RiskLevel riskLevel, List<RiskAssessmentResponse.TriggeredRule> triggeredRules) {
        String ruleNames = triggeredRules.stream()
                .map(RiskAssessmentResponse.TriggeredRule::getRuleName)
                .limit(3)
                .collect(Collectors.joining(", "));
        return String.format("[%s] Fraud risk detected: %s", riskLevel, ruleNames);
    }

    private String generateAlertDescription(RiskLevel riskLevel, List<RiskAssessmentResponse.TriggeredRule> triggeredRules) {
        StringBuilder sb = new StringBuilder();
        sb.append("Risk level: ").append(riskLevel).append("\n");
        sb.append("Triggered rules:\n");
        for (RiskAssessmentResponse.TriggeredRule tr : triggeredRules) {
            sb.append("- ").append(tr.getRuleName())
                    .append(" (").append(tr.getRuleType()).append(")")
                    .append(" impact: ").append(tr.getScoreImpact()).append("\n");
        }
        return sb.toString();
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Transactional(readOnly = true)
    public List<RiskScore> findByCustomerId(UUID customerId) {
        return riskScoreRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public Optional<RiskScore> findLatestByTransactionId(UUID transactionId) {
        return riskScoreRepository.findLatestByTransactionId(transactionId);
    }

    @Transactional(readOnly = true)
    public Double getAverageScore(UUID customerId, LocalDateTime since) {
        return riskScoreRepository.getAverageScore(customerId, since);
    }
}