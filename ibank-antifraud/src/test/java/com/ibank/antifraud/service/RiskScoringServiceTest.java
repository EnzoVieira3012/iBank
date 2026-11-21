package com.ibank.antifraud.service;

import com.ibank.antifraud.controller.dto.RiskAssessmentResponse;
import com.ibank.antifraud.controller.dto.RiskScoreRequest;
import com.ibank.antifraud.model.FraudRule;
import com.ibank.antifraud.model.FraudRuleAction;
import com.ibank.antifraud.model.FraudRuleType;
import com.ibank.antifraud.model.RiskLevel;
import com.ibank.antifraud.repository.FraudAlertRepository;
import com.ibank.antifraud.repository.FraudRuleRepository;
import com.ibank.antifraud.repository.RiskScoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskScoringServiceTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @Mock
    private RiskScoreRepository riskScoreRepository;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private DeviceFingerprintService deviceFingerprintService;

    @InjectMocks
    private RiskScoringService riskScoringService;

    @Test
    void assessRisk_noRules_shouldReturnLowRisk() {
        when(fraudRuleRepository.findApplicableRules(any(), any(), any(), any()))
                .thenReturn(List.of());

        RiskScoreRequest request = RiskScoreRequest.builder()
                .customerId(UUID.randomUUID().toString())
                .build();

        RiskAssessmentResponse response = riskScoringService.assessRisk(request);

        assertEquals(RiskLevel.VERY_LOW, response.getRiskLevel());
        assertEquals(FraudRuleAction.ALLOW, response.getRecommendedAction());
        assertTrue(response.getTriggeredRules().isEmpty());
        assertEquals(0, response.getScore());
    }

    @Test
    void assessRisk_withTriggeredRules_shouldCalculateScore() {
        FraudRule rule1 = FraudRule.builder()
                .id(UUID.randomUUID())
                .name("High Amount Rule")
                .ruleType(FraudRuleType.AMOUNT_THRESHOLD)
                .action(FraudRuleAction.ALERT)
                .scoreImpact(200)
                .validFrom(LocalDate.now().minusDays(1))
                .active(true)
                .build();

        FraudRule rule2 = FraudRule.builder()
                .id(UUID.randomUUID())
                .name("Velocity Rule")
                .ruleType(FraudRuleType.VELOCITY)
                .action(FraudRuleAction.REVIEW)
                .scoreImpact(300)
                .validFrom(LocalDate.now().minusDays(1))
                .active(true)
                .build();

        when(fraudRuleRepository.findApplicableRules(any(), any(), any(), any()))
                .thenReturn(List.of(rule1, rule2));
        when(fraudAlertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(riskScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RiskScoreRequest request = RiskScoreRequest.builder()
                .customerId(UUID.randomUUID().toString())
                .build();

        RiskAssessmentResponse response = riskScoringService.assessRisk(request);

        assertEquals(500, response.getScore());
        assertEquals(RiskLevel.VERY_HIGH, response.getRiskLevel());
        assertEquals(FraudRuleAction.REVIEW, response.getRecommendedAction());
        assertEquals(2, response.getTriggeredRules().size());
    }

    @Test
    void assessRisk_criticalScore_shouldBlock() {
        FraudRule rule = FraudRule.builder()
                .id(UUID.randomUUID())
                .name("Critical Rule")
                .ruleType(FraudRuleType.FRAUD_RULE)
                .action(FraudRuleAction.BLOCK)
                .scoreImpact(800)
                .validFrom(LocalDate.now().minusDays(1))
                .active(true)
                .build();

        when(fraudRuleRepository.findApplicableRules(any(), any(), any(), any()))
                .thenReturn(List.of(rule));
        when(fraudAlertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(riskScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RiskScoreRequest request = RiskScoreRequest.builder()
                .customerId(UUID.randomUUID().toString())
                .build();

        RiskAssessmentResponse response = riskScoringService.assessRisk(request);

        assertEquals(RiskLevel.CRITICAL, response.getRiskLevel());
        assertEquals(FraudRuleAction.BLOCK, response.getRecommendedAction());
        assertTrue(response.isRequiresReview());
    }
}