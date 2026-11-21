package com.ibank.reconciliation.service;

import com.ibank.reconciliation.model.ReconciliationBreak;
import com.ibank.reconciliation.model.ReconciliationJob;
import com.ibank.reconciliation.model.ReconciliationRule;
import com.ibank.reconciliation.repository.ReconciliationBreakRepository;
import com.ibank.reconciliation.repository.ReconciliationJobRepository;
import com.ibank.reconciliation.repository.ReconciliationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final ReconciliationJobRepository jobRepository;
    private final ReconciliationBreakRepository breakRepository;
    private final ReconciliationRuleRepository ruleRepository;

    @Transactional
    public ReconciliationJob createJob(String jobType, String sourceSystem, String targetSystem,
                                        String externalReference, Map<String, Object> parameters) {
        ReconciliationJob job = ReconciliationJob.builder()
            .jobType(jobType)
            .status(ReconciliationJob.Status.PENDING.name())
            .sourceSystem(sourceSystem)
            .targetSystem(targetSystem)
            .externalReference(externalReference)
            .parameters(parameters)
            .build();
        return jobRepository.save(job);
    }

    @Transactional
    public ReconciliationJob startJob(UUID jobId) {
        ReconciliationJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.setStatus(ReconciliationJob.Status.RUNNING.name());
        job.setStartedAt(Instant.now());
        return jobRepository.save(job);
    }

    @Transactional
    public ReconciliationJob completeJob(UUID jobId, long total, long matched, long mismatched,
                                         long missingSource, long missingTarget, long breaks) {
        ReconciliationJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.setTotalRecords(total);
        job.setMatchedRecords(matched);
        job.setMismatchedRecords(mismatched);
        job.setMissingInSource(missingSource);
        job.setMissingInTarget(missingTarget);
        job.setBreaksCount(breaks);
        job.setCompletedAt(Instant.now());
        job.setStatus(breaks > 0
            ? ReconciliationJob.Status.COMPLETED_WITH_BREAKS.name()
            : ReconciliationJob.Status.COMPLETED.name());
        return jobRepository.save(job);
    }

    @Transactional
    public ReconciliationJob failJob(UUID jobId, String error) {
        ReconciliationJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.setStatus(ReconciliationJob.Status.FAILED.name());
        job.setCompletedAt(Instant.now());
        job.setErrorMessage(error);
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationJob> getJobs(String status, String jobType, Instant start, Instant end, Pageable pageable) {
        if (status != null && jobType != null) {
            return jobRepository.findByJobType(jobType, pageable);
        } else if (status != null) {
            return jobRepository.findByStatus(status, pageable);
        } else if (start != null && end != null) {
            return jobRepository.findByStartedAtBetween(start, end, pageable);
        }
        return jobRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<ReconciliationJob> getLatestJob(String jobType) {
        return jobRepository.findLatestByJobType(jobType);
    }

    @Transactional(readOnly = true)
    public long countActiveJobs() {
        return jobRepository.countActiveJobs();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getJobStatusStats(Instant start, Instant end) {
        return jobRepository.getStatusStats(start, end);
    }

    @Transactional
    public ReconciliationBreak createBreak(ReconciliationJob job, ReconciliationBreak.BreakType breakType,
                                           String entityType, String entityId,
                                           String sourceRef, String targetRef,
                                           BigDecimal sourceAmt, BigDecimal targetAmt,
                                           String currency, Map<String, Object> sourceData,
                                           Map<String, Object> targetData) {
        BigDecimal diff = BigDecimal.ZERO;
        if (sourceAmt != null && targetAmt != null) {
            diff = sourceAmt.subtract(targetAmt).abs();
        }

        ReconciliationBreak.BreakType type = breakType != null ? breakType : inferBreakType(sourceAmt, targetAmt, sourceRef, targetRef);
        ReconciliationBreak.Priority priority = calculatePriority(type, diff, currency);

        ReconciliationBreak brk = ReconciliationBreak.builder()
            .job(job)
            .breakType(type.name())
            .entityType(entityType)
            .entityId(entityId)
            .sourceReference(sourceRef)
            .targetReference(targetRef)
            .sourceAmount(sourceAmt)
            .targetAmount(targetAmt)
            .differenceAmount(diff)
            .currency(currency)
            .status(ReconciliationBreak.Status.OPEN.name())
            .priority(priority.name())
            .sourceData(sourceData)
            .targetData(targetData)
            .dueDate(calculateDueDate(priority))
            .build();

        ReconciliationBreak saved = breakRepository.save(brk);

        job.setBreaksCount(job.getBreaksCount() + 1);
        jobRepository.save(job);

        return saved;
    }

    private ReconciliationBreak.BreakType inferBreakType(BigDecimal sourceAmt, BigDecimal targetAmt,
                                                          String sourceRef, String targetRef) {
        if (sourceAmt != null && targetAmt != null && sourceAmt.compareTo(targetAmt) != 0) {
            return ReconciliationBreak.BreakType.AMOUNT_MISMATCH;
        }
        if (sourceRef == null && targetRef != null) {
            return ReconciliationBreak.BreakType.MISSING_IN_SOURCE;
        }
        if (sourceRef != null && targetRef == null) {
            return ReconciliationBreak.BreakType.MISSING_IN_TARGET;
        }
        return ReconciliationBreak.BreakType.REFERENCE_MISMATCH;
    }

    private ReconciliationBreak.Priority calculatePriority(ReconciliationBreak.BreakType type,
                                                            BigDecimal diff, String currency) {
        if (type == ReconciliationBreak.BreakType.AMOUNT_MISMATCH && diff != null) {
            if (diff.compareTo(new BigDecimal("100000")) > 0) return ReconciliationBreak.Priority.CRITICAL;
            if (diff.compareTo(new BigDecimal("10000")) > 0) return ReconciliationBreak.Priority.HIGH;
        }
        if (type == ReconciliationBreak.BreakType.MISSING_IN_SOURCE ||
            type == ReconciliationBreak.BreakType.MISSING_IN_TARGET) {
            return ReconciliationBreak.Priority.HIGH;
        }
        return ReconciliationBreak.Priority.MEDIUM;
    }

    private Instant calculateDueDate(ReconciliationBreak.Priority priority) {
        Instant now = Instant.now();
        return switch (priority) {
            case CRITICAL -> now.plusSeconds(2 * 60 * 60); // 2 hours
            case HIGH -> now.plusSeconds(8 * 60 * 60); // 8 hours
            case MEDIUM -> now.plusSeconds(24 * 60 * 60); // 24 hours
            case LOW -> now.plusSeconds(72 * 60 * 60); // 72 hours
        };
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationBreak> getBreaksByJob(UUID jobId, Pageable pageable) {
        return breakRepository.findByJobId(jobId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationBreak> getBreaksByStatus(String status, Pageable pageable) {
        return breakRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationBreak> getOpenBreaksPrioritized(UUID jobId) {
        return breakRepository.findOpenBreaksPrioritized(jobId);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationBreak> getOverdueBreaks() {
        return breakRepository.findOverdueBreaks(Instant.now());
    }

    @Transactional(readOnly = true)
    public long countOpenBreaks(UUID jobId) {
        return breakRepository.countOpenBreaksByJob(jobId);
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationBreak> getBreaksByAssignee(String assigneeId, Pageable pageable) {
        return breakRepository.findByStatusAndAssigneeId("OPEN", assigneeId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getBreakTypeStats(UUID jobId) {
        return breakRepository.getBreakTypeStatsByJob(jobId);
    }

    @Transactional
    public ReconciliationBreak resolveBreak(UUID breakId, String resolvedBy, String notes,
                                            Map<String, Object> resolutionData) {
        ReconciliationBreak brk = breakRepository.findById(breakId)
            .orElseThrow(() -> new IllegalArgumentException("Break not found: " + breakId));

        brk.setStatus(ReconciliationBreak.Status.RESOLVED.name());
        brk.setResolvedBy(resolvedBy);
        brk.setResolvedAt(Instant.now());
        brk.setResolutionNotes(notes);
        brk.setResolutionData(resolutionData);

        ReconciliationBreak saved = breakRepository.save(brk);

        ReconciliationJob job = saved.getJob();
        job.setResolvedBreaks(job.getResolvedBreaks() + 1);
        jobRepository.save(job);

        return saved;
    }

    @Transactional
    public ReconciliationBreak assignBreak(UUID breakId, String assigneeId) {
        ReconciliationBreak brk = breakRepository.findById(breakId)
            .orElseThrow(() -> new IllegalArgumentException("Break not found: " + breakId));
        brk.setAssigneeId(assigneeId);
        brk.setStatus(ReconciliationBreak.Status.IN_PROGRESS.name());
        return breakRepository.save(brk);
    }

    @Transactional
    public ReconciliationBreak escalateBreak(UUID breakId, String notes) {
        ReconciliationBreak brk = breakRepository.findById(breakId)
            .orElseThrow(() -> new IllegalArgumentException("Break not found: " + breakId));
        brk.setStatus(ReconciliationBreak.Status.ESCALATED.name());
        brk.setResolutionNotes(notes);
        brk.setPriority(ReconciliationBreak.Priority.CRITICAL.name());
        brk.setDueDate(Instant.now().plusSeconds(2 * 60 * 60));
        return breakRepository.save(brk);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRule> getRulesForJob(String jobType) {
        return ruleRepository.findByJobTypeAndActiveTrueOrderByPriorityDesc(jobType);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRule> getRulesForEntity(String entityType) {
        return ruleRepository.findByEntityTypeAndActiveTrueOrderByPriorityDesc(entityType);
    }

    @Transactional(readOnly = true)
    public ReconciliationDashboard getDashboardSummary() {
        long totalJobs = jobRepository.count();
        long activeJobs = countActiveJobs();
        long openBreaks = breakRepository.countByStatus("OPEN", org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        long inProgressBreaks = breakRepository.countByStatus("IN_PROGRESS", org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        long overdueBreaks = getOverdueBreaks().size();
        return new ReconciliationDashboard(totalJobs, activeJobs, openBreaks, inProgressBreaks, overdueBreaks, Instant.now());
    }

    public record ReconciliationDashboard(
        long totalJobs,
        long activeJobs,
        long openBreaks,
        long inProgressBreaks,
        long overdueBreaks,
        Instant generatedAt
    ) {}
}