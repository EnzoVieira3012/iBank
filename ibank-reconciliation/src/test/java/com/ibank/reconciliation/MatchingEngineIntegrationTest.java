package com.ibank.reconciliation;

import com.ibank.reconciliation.model.ReconciliationBreak;
import com.ibank.reconciliation.model.ReconciliationJob;
import com.ibank.reconciliation.model.ReconciliationRule;
import com.ibank.reconciliation.repository.ReconciliationJobRepository;
import com.ibank.reconciliation.repository.ReconciliationRuleRepository;
import com.ibank.reconciliation.service.MatchingEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatchingEngineIntegrationTest {

    @Autowired
    private MatchingEngine matchingEngine;

    @Autowired
    private ReconciliationJobRepository jobRepository;

    @Autowired
    private ReconciliationRuleRepository ruleRepository;

    @Test
    void executeMatch_shouldMatchRecordsOnReference() {
        ReconciliationJob job = ReconciliationJob.builder()
            .jobType("DAILY_SETTLEMENT")
            .status("RUNNING")
            .sourceSystem("core")
            .targetSystem("gl")
            .build();
        job = jobRepository.save(job);

        List<Map<String, Object>> source = List.of(
            Map.of("entityType", "Transfer", "entityId", "txn-1", "reference", "ref-1", "amount", 100.00, "currency", "BRL", "status", "COMPLETED"),
            Map.of("entityType", "Transfer", "entityId", "txn-2", "reference", "ref-2", "amount", 200.00, "currency", "BRL", "status", "COMPLETED")
        );

        List<Map<String, Object>> target = List.of(
            Map.of("entityType", "Transfer", "entityId", "txn-1", "reference", "ref-1", "amount", 100.00, "currency", "BRL", "status", "COMPLETED"),
            Map.of("entityType", "Transfer", "entityId", "txn-2", "reference", "ref-2", "amount", 200.00, "currency", "BRL", "status", "COMPLETED")
        );

        MatchingEngine.MatchResult result = matchingEngine.executeMatch(job, source, target);

        assertThat(result.matched()).hasSize(2);
        assertThat(result.missingInSource()).isEmpty();
        assertThat(result.missingInTarget()).isEmpty();
        assertThat(result.breaks()).isEmpty();
    }

    @Test
    void executeMatch_shouldDetectAmountMismatch() {
        ReconciliationJob job = ReconciliationJob.builder()
            .jobType("DAILY_SETTLEMENT")
            .status("RUNNING")
            .sourceSystem("core")
            .targetSystem("gl")
            .build();
        job = jobRepository.save(job);

        List<Map<String, Object>> source = List.of(
            Map.of("entityType", "Transfer", "entityId", "txn-1", "reference", "ref-1", "amount", 100.00, "currency", "BRL")
        );

        List<Map<String, Object>> target = List.of(
            Map.of("entityType", "Transfer", "entityId", "txn-1", "reference", "ref-1", "amount", 100.50, "currency", "BRL")
        );

        MatchingEngine.MatchResult result = matchingEngine.executeMatch(job, source, target);

        assertThat(result.matched()).hasSize(1);
        assertThat(result.breaks()).hasSize(1);
        assertThat(result.breaks().get(0).getBreakType()).isEqualTo("AMOUNT_MISMATCH");
    }

    @Test
    void executeMatch_shouldDetectMissingInTarget() {
        ReconciliationJob job = ReconciliationJob.builder()
            .jobType("DAILY_SETTLEMENT")
            .status("RUNNING")
            .sourceSystem("core")
            .targetSystem("gl")
            .build();
        job = jobRepository.save(job);

        List<Map<String, Object>> source = List.of(
            Map.of("entityType", "Transfer", "entityId", "txn-1", "reference", "ref-1", "amount", 100.00)
        );

        List<Map<String, Object>> target = List.of();

        MatchingEngine.MatchResult result = matchingEngine.executeMatch(job, source, target);

        assertThat(result.matched()).isEmpty();
        assertThat(result.missingInTarget()).hasSize(1);
        assertThat(result.breaks()).hasSize(1);
        assertThat(result.breaks().get(0).getBreakType()).isEqualTo("MISSING_IN_TARGET");
    }

    @Test
    void executeMatch_shouldDetectMissingInSource() {
        ReconciliationJob job = ReconciliationJob.builder()
            .jobType("DAILY_SETTLEMENT")
            .status("RUNNING")
            .sourceSystem("core")
            .targetSystem("gl")
            .build();
        job = jobRepository.save(job);

        List<Map<String, Object>> source = List.of();
        List<Map<String, Object>> target = List.of(
            Map.of("entityType", "Transfer", "entityId", "txn-1", "reference", "ref-1", "amount", 100.00)
        );

        MatchingEngine.MatchResult result = matchingEngine.executeMatch(job, source, target);

        assertThat(result.matched()).isEmpty();
        assertThat(result.missingInSource()).hasSize(1);
        assertThat(result.breaks()).hasSize(1);
        assertThat(result.breaks().get(0).getBreakType()).isEqualTo("MISSING_IN_SOURCE");
    }

    @Test
    void executeMatch_shouldRespectTolerance() {
        ReconciliationJob job = ReconciliationJob.builder()
            .jobType("DAILY_SETTLEMENT")
            .status("RUNNING")
            .sourceSystem("core")
            .targetSystem("gl")
            .build();
        job = jobRepository.save(job);

        // Small difference within 0.01 absolute tolerance
        List<Map<String, Object>> source = List.of(
            Map.of("entityType", "Transfer", "entityId", "txn-1", "reference", "ref-1", "amount", 100.00, "currency", "BRL")
        );

        List<Map<String, Object>> target = List.of(
            Map.of("entityType", "Transfer", "entityId", "txn-1", "reference", "ref-1", "amount", 100.005, "currency", "BRL")
        );

        MatchingEngine.MatchResult result = matchingEngine.executeMatch(job, source, target);

        assertThat(result.matched()).hasSize(1);
        assertThat(result.breaks()).isEmpty(); // Within tolerance
    }

    @Test
    void executeMatch_shouldDetectCurrencyMismatch() {
        ReconciliationJob job = ReconciliationJob.builder()
            .jobType("DAILY_SETTLEMENT")
            .status("RUNNING")
            .sourceSystem("core")
            .targetSystem("gl")
            .build();
        job = jobRepository.save(job);

        List<Map<String, Object>> source = List.of(
            Map.of("entityType", "Transfer", "entityId", "txn-1", "reference", "ref-1", "amount", 100.00, "currency", "BRL")
        );

        List<Map<String, Object>> target = List.of(
            Map.of("entityType", "Transfer", "entityId", "txn-1", "reference", "ref-1", "amount", 100.00, "currency", "USD")
        );

        MatchingEngine.MatchResult result = matchingEngine.executeMatch(job, source, target);

        assertThat(result.matched()).hasSize(1);
        assertThat(result.breaks()).hasSize(1);
        assertThat(result.breaks().get(0).getBreakType()).isEqualTo("CURRENCY_MISMATCH");
    }
}