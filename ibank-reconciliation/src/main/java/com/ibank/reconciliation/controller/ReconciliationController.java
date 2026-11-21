package com.ibank.reconciliation.controller;

import com.ibank.reconciliation.dto.ReconciliationBreakResponse;
import com.ibank.reconciliation.dto.ReconciliationJobResponse;
import com.ibank.reconciliation.dto.ReconciliationRuleResponse;
import com.ibank.reconciliation.mapper.ReconciliationBreakMapper;
import com.ibank.reconciliation.mapper.ReconciliationJobMapper;
import com.ibank.reconciliation.mapper.ReconciliationRuleMapper;
import com.ibank.reconciliation.model.ReconciliationBreak;
import com.ibank.reconciliation.model.ReconciliationJob;
import com.ibank.reconciliation.model.ReconciliationRule;
import com.ibank.reconciliation.service.MatchingEngine;
import com.ibank.reconciliation.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
@Tag(name = "Reconciliation", description = "Reconciliation and break management APIs")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private final MatchingEngine matchingEngine;
    private final ReconciliationJobMapper jobMapper;
    private final ReconciliationBreakMapper breakMapper;
    private final ReconciliationRuleMapper ruleMapper;

    @PostMapping("/jobs")
    @PreAuthorize("hasRole('RECONCILIATION_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Create a new reconciliation job")
    public ResponseEntity<ReconciliationJobResponse> createJob(
        @RequestParam String jobType,
        @RequestParam String sourceSystem,
        @RequestParam String targetSystem,
        @RequestParam(required = false) String externalReference,
        @RequestBody(required = false) Map<String, Object> parameters
    ) {
        ReconciliationJob job = reconciliationService.createJob(
            jobType, sourceSystem, targetSystem, externalReference, parameters
        );
        return ResponseEntity.ok(jobMapper.toResponse(job));
    }

    @PostMapping("/jobs/{jobId}/run")
    @PreAuthorize("hasRole('RECONCILIATION_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Run a reconciliation job with provided data")
    public ResponseEntity<ReconciliationJobResponse> runJob(
        @PathVariable UUID jobId,
        @RequestBody MatchRequest request
    ) {
        ReconciliationJob job = reconciliationService.startJob(jobId);

        MatchingEngine.MatchResult result = matchingEngine.executeMatch(
            job, request.sourceRecords(), request.targetRecords()
        );

        for (ReconciliationBreak brk : result.breaks()) {
            reconciliationService.createBreak(
                job,
                ReconciliationBreak.BreakType.valueOf(brk.getBreakType()),
                brk.getEntityType(),
                brk.getEntityId(),
                brk.getSourceReference(),
                brk.getTargetReference(),
                brk.getSourceAmount(),
                brk.getTargetAmount(),
                brk.getCurrency(),
                brk.getSourceData(),
                brk.getTargetData()
            );
        }

        ReconciliationJob completed = reconciliationService.completeJob(
            jobId,
            request.sourceRecords().size() + request.targetRecords().size(),
            result.matched().size(),
            result.missingInSource().size() + result.missingInTarget().size(),
            result.missingInSource().size(),
            result.missingInTarget().size(),
            result.breaks().size()
        );

        return ResponseEntity.ok(jobMapper.toResponse(completed));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasRole('RECONCILIATION_VIEW') or hasRole('ADMIN')")
    @Operation(summary = "List reconciliation jobs")
    public ResponseEntity<Page<ReconciliationJobResponse>> listJobs(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String jobType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "startedAt") String sort,
        @RequestParam(defaultValue = "DESC") String direction
    ) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.fromString(direction), sort));

        Page<ReconciliationJob> jobs = reconciliationService.getJobs(status, jobType, start, end, pageable);
        return ResponseEntity.ok(jobs.map(jobMapper::toResponse));
    }

    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasRole('RECONCILIATION_VIEW') or hasRole('ADMIN')")
    @Operation(summary = "Get reconciliation job by ID")
    public ResponseEntity<ReconciliationJobResponse> getJob(@PathVariable UUID jobId) {
        return reconciliationService.getJobs(null, null, null, null, Pageable.unpaged())
            .getContent().stream()
            .filter(j -> j.getId().equals(jobId))
            .findFirst()
            .map(jobMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs/latest/{jobType}")
    @PreAuthorize("hasRole('RECONCILIATION_VIEW') or hasRole('ADMIN')")
    @Operation(summary = "Get latest job for a type")
    public ResponseEntity<ReconciliationJobResponse> getLatestJob(@PathVariable String jobType) {
        return reconciliationService.getLatestJob(jobType)
            .map(jobMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs/{jobId}/breaks")
    @PreAuthorize("hasRole('RECONCILIATION_VIEW') or hasRole('ADMIN')")
    @Operation(summary = "Get breaks for a job")
    public ResponseEntity<Page<ReconciliationBreakResponse>> getJobBreaks(
        @PathVariable UUID jobId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReconciliationBreak> breaks = status != null
            ? reconciliationService.getBreaksByStatus(status, pageable)
            : reconciliationService.getBreaksByJob(jobId, pageable);
        return ResponseEntity.ok(breaks.map(breakMapper::toResponse));
    }

    @GetMapping("/breaks")
    @PreAuthorize("hasRole('RECONCILIATION_VIEW') or hasRole('ADMIN')")
    @Operation(summary = "List all breaks with filters")
    public ResponseEntity<Page<ReconciliationBreakResponse>> listBreaks(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String breakType,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) String entityId,
        @RequestParam(required = false) String assigneeId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(defaultValue = "DESC") String direction
    ) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.fromString(direction), sort));

        Page<ReconciliationBreak> breaks;
        if (status != null) {
            breaks = reconciliationService.getBreaksByStatus(status, pageable);
        } else if (assigneeId != null) {
            breaks = reconciliationService.getBreaksByAssignee(assigneeId, pageable);
        } else {
            breaks = reconciliationService.getBreaksByStatus("OPEN", pageable);
        }
        return ResponseEntity.ok(breaks.map(breakMapper::toResponse));
    }

    @GetMapping("/breaks/overdue")
    @PreAuthorize("hasRole('RECONCILIATION_VIEW') or hasRole('ADMIN')")
    @Operation(summary = "Get overdue breaks")
    public ResponseEntity<List<ReconciliationBreakResponse>> getOverdueBreaks() {
        List<ReconciliationBreak> breaks = reconciliationService.getOverdueBreaks();
        return ResponseEntity.ok(breaks.stream().map(breakMapper::toResponse).toList());
    }

    @GetMapping("/breaks/prioritized/{jobId}")
    @PreAuthorize("hasRole('RECONCILIATION_VIEW') or hasRole('ADMIN')")
    @Operation(summary = "Get open breaks prioritized for a job")
    public ResponseEntity<List<ReconciliationBreakResponse>> getPrioritizedBreaks(@PathVariable UUID jobId) {
        List<ReconciliationBreak> breaks = reconciliationService.getOpenBreaksPrioritized(jobId);
        return ResponseEntity.ok(breaks.stream().map(breakMapper::toResponse).toList());
    }

    @PostMapping("/breaks/{breakId}/assign")
    @PreAuthorize("hasRole('RECONCILIATION_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Assign break to user")
    public ResponseEntity<ReconciliationBreakResponse> assignBreak(
        @PathVariable UUID breakId,
        @RequestParam String assigneeId
    ) {
        ReconciliationBreak brk = reconciliationService.assignBreak(breakId, assigneeId);
        return ResponseEntity.ok(breakMapper.toResponse(brk));
    }

    @PostMapping("/breaks/{breakId}/resolve")
    @PreAuthorize("hasRole('RECONCILIATION_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Resolve a break")
    public ResponseEntity<ReconciliationBreakResponse> resolveBreak(
        @PathVariable UUID breakId,
        @RequestParam String resolvedBy,
        @RequestParam(required = false) String notes,
        @RequestBody(required = false) Map<String, Object> resolutionData
    ) {
        ReconciliationBreak brk = reconciliationService.resolveBreak(breakId, resolvedBy, notes, resolutionData);
        return ResponseEntity.ok(breakMapper.toResponse(brk));
    }

    @PostMapping("/breaks/{breakId}/escalate")
    @PreAuthorize("hasRole('RECONCILIATION_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Escalate a break")
    public ResponseEntity<ReconciliationBreakResponse> escalateBreak(
        @PathVariable UUID breakId,
        @RequestParam(required = false) String notes
    ) {
        ReconciliationBreak brk = reconciliationService.escalateBreak(breakId, notes);
        return ResponseEntity.ok(breakMapper.toResponse(brk));
    }

    @GetMapping("/rules")
    @PreAuthorize("hasRole('RECONCILIATION_VIEW') or hasRole('ADMIN')")
    @Operation(summary = "List reconciliation rules")
    public ResponseEntity<List<ReconciliationRuleResponse>> listRules(
        @RequestParam(required = false) String jobType,
        @RequestParam(required = false) String entityType
    ) {
        List<ReconciliationRule> rules;
        if (jobType != null && entityType != null) {
            rules = reconciliationService.getRulesForJob(jobType).stream()
                .filter(r -> r.getEntityType().equals(entityType))
                .toList();
        } else if (jobType != null) {
            rules = reconciliationService.getRulesForJob(jobType);
        } else if (entityType != null) {
            rules = reconciliationService.getRulesForEntity(entityType);
        } else {
            rules = List.of();
        }
        return ResponseEntity.ok(rules.stream().map(ruleMapper::toResponse).toList());
    }

    public record MatchRequest(
        List<Map<String, Object>> sourceRecords,
        List<Map<String, Object>> targetRecords
    ) {}
}