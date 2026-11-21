package com.ibank.reconciliation;

import com.ibank.reconciliation.model.ReconciliationBreak;
import com.ibank.reconciliation.model.ReconciliationJob;
import com.ibank.reconciliation.repository.ReconciliationBreakRepository;
import com.ibank.reconciliation.repository.ReconciliationJobRepository;
import com.ibank.reconciliation.service.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReconciliationServiceIntegrationTest {

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private ReconciliationJobRepository jobRepository;

    @Autowired
    private ReconciliationBreakRepository breakRepository;

    @Test
    void createJob_shouldPersistJob() {
        ReconciliationJob job = reconciliationService.createJob(
            "DAILY_SETTLEMENT", "core-banking", "general-ledger",
            "TEST-REF-1", Map.of("date", "2024-01-01")
        );

        assertThat(job.getId()).isNotNull();
        assertThat(job.getJobType()).isEqualTo("DAILY_SETTLEMENT");
        assertThat(job.getStatus()).isEqualTo("PENDING");
        assertThat(job.getExternalReference()).isEqualTo("TEST-REF-1");
    }

    @Test
    void startAndCompleteJob_shouldUpdateStatus() {
        ReconciliationJob job = reconciliationService.createJob(
            "DAILY_SETTLEMENT", "core", "gl", "ref-1", Map.of()
        );

        reconciliationService.startJob(job.getId());
        ReconciliationJob started = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(started.getStatus()).isEqualTo("RUNNING");
        assertThat(started.getStartedAt()).isNotNull();

        reconciliationService.completeJob(job.getId(), 100, 95, 3, 1, 1, 2);
        ReconciliationJob completed = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo("COMPLETED_WITH_BREAKS");
        assertThat(completed.getTotalRecords()).isEqualTo(100);
        assertThat(completed.getMatchedRecords()).isEqualTo(95);
        assertThat(completed.getBreaksCount()).isEqualTo(2);
        assertThat(completed.getCompletedAt()).isNotNull();
    }

    @Test
    void createBreak_shouldIncrementJobBreakCount() {
        ReconciliationJob job = reconciliationService.createJob(
            "DAILY_SETTLEMENT", "core", "gl", "ref-1", Map.of()
        );
        reconciliationService.startJob(job.getId());

        ReconciliationBreak brk = reconciliationService.createBreak(
            job,
            ReconciliationBreak.BreakType.AMOUNT_MISMATCH,
            "Transfer",
            "txn-123",
            "src-ref",
            "tgt-ref",
            new BigDecimal("100.00"),
            new BigDecimal("100.50"),
            "BRL",
            Map.of("amount", 100.00),
            Map.of("amount", 100.50)
        );

        assertThat(brk.getId()).isNotNull();
        assertThat(brk.getBreakType()).isEqualTo("AMOUNT_MISMATCH");
        assertThat(brk.getDifferenceAmount()).isEqualByComparingTo("0.50");
        assertThat(brk.getPriority()).isEqualTo("MEDIUM");
        assertThat(brk.getDueDate()).isNotNull();

        ReconciliationJob updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getBreaksCount()).isEqualTo(1);
    }

    @Test
    void resolveBreak_shouldUpdateJobResolvedCount() {
        ReconciliationJob job = reconciliationService.createJob(
            "DAILY_SETTLEMENT", "core", "gl", "ref-1", Map.of()
        );
        reconciliationService.startJob(job.getId());

        ReconciliationBreak brk = reconciliationService.createBreak(
            job, ReconciliationBreak.BreakType.AMOUNT_MISMATCH, "Transfer", "txn-1",
            "s1", "t1", new BigDecimal("100"), new BigDecimal("101"), "BRL", Map.of(), Map.of()
        );

        reconciliationService.resolveBreak(brk.getId(), "user-1", "Fixed", Map.of("adjustment", 1));

        ReconciliationBreak resolved = breakRepository.findById(brk.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo("RESOLVED");
        assertThat(resolved.getResolvedBy()).isEqualTo("user-1");
        assertThat(resolved.getResolvedAt()).isNotNull();

        ReconciliationJob updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getResolvedBreaks()).isEqualTo(1);
    }

    @Test
    void getOpenBreaksPrioritized_shouldOrderByPriority() {
        ReconciliationJob job = reconciliationService.createJob(
            "DAILY_SETTLEMENT", "core", "gl", "ref-1", Map.of()
        );
        reconciliationService.startJob(job.getId());

        // Create breaks with different priorities
        reconciliationService.createBreak(job, ReconciliationBreak.BreakType.AMOUNT_MISMATCH, "T", "1", "s", "t", new BigDecimal("50000"), new BigDecimal("51000"), "BRL", Map.of(), Map.of()); // HIGH
        reconciliationService.createBreak(job, ReconciliationBreak.BreakType.MISSING_IN_SOURCE, "T", "2", null, "t", null, new BigDecimal("100"), "BRL", Map.of(), Map.of()); // HIGH
        reconciliationService.createBreak(job, ReconciliationBreak.BreakType.AMOUNT_MISMATCH, "T", "3", "s", "t", new BigDecimal("100"), new BigDecimal("101"), "BRL", Map.of(), Map.of()); // MEDIUM

        List<ReconciliationBreak> prioritized = reconciliationService.getOpenBreaksPrioritized(job.getId());

        assertThat(prioritized).hasSize(3);
        assertThat(prioritized.get(0).getPriority()).isEqualTo("HIGH");
        assertThat(prioritized.get(1).getPriority()).isEqualTo("HIGH");
        assertThat(prioritized.get(2).getPriority()).isEqualTo("MEDIUM");
    }

    @Test
    void escalateBreak_shouldSetCriticalPriority() {
        ReconciliationJob job = reconciliationService.createJob(
            "DAILY_SETTLEMENT", "core", "gl", "ref-1", Map.of()
        );
        reconciliationService.startJob(job.getId());

        ReconciliationBreak brk = reconciliationService.createBreak(
            job, ReconciliationBreak.BreakType.AMOUNT_MISMATCH, "T", "1", "s", "t", new BigDecimal("100"), new BigDecimal("101"), "BRL", Map.of(), Map.of()
        );

        reconciliationService.escalateBreak(brk.getId(), "Needs attention");

        ReconciliationBreak escalated = breakRepository.findById(brk.getId()).orElseThrow();
        assertThat(escalated.getStatus()).isEqualTo("ESCALATED");
        assertThat(escalated.getPriority()).isEqualTo("CRITICAL");
    }
}