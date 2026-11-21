package com.ibank.audit.controller;

import com.ibank.audit.dto.AuditEventResponse;
import com.ibank.audit.dto.AuditQueryRequest;
import com.ibank.audit.dto.AuditStatisticsResponse;
import com.ibank.audit.mapper.AuditEventMapper;
import com.ibank.audit.model.AuditEvent;
import com.ibank.audit.service.AuditService;
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
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit trail and compliance APIs")
public class AuditController {

    private final AuditService auditService;
    private final AuditEventMapper mapper;

    @GetMapping("/events/{id}")
    @PreAuthorize("hasRole('AUDITOR') or hasRole('ADMIN')")
    @Operation(summary = "Get audit event by ID")
    public ResponseEntity<AuditEventResponse> getEvent(
        @Parameter(description = "Event UUID") @PathVariable UUID id
    ) {
        return auditService.findById(id)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/events")
    @PreAuthorize("hasRole('AUDITOR') or hasRole('ADMIN')")
    @Operation(summary = "Query audit events")
    public ResponseEntity<Page<AuditEventResponse>> queryEvents(
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) String entityId,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) String correlationId,
        @RequestParam(required = false) List<String> eventTypes,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "occurredAt") String sort,
        @RequestParam(defaultValue = "DESC") String direction
    ) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.fromString(direction), sort));

        Page<AuditEvent> events;
        if (entityType != null && entityId != null) {
            events = auditService.getEntityHistory(entityType, entityId, pageable);
        } else if (userId != null) {
            events = auditService.getUserActivity(userId, pageable);
        } else if (correlationId != null) {
            events = auditService.getCorrelationTrail(correlationId, pageable);
        } else if (start != null && end != null) {
            events = auditService.getEventsByPeriod(start, end, pageable);
        } else if (eventTypes != null && !eventTypes.isEmpty()) {
            events = auditService.getEventsByTypes(eventTypes, pageable);
        } else {
            events = Page.empty(pageable);
        }

        return ResponseEntity.ok(events.map(mapper::toResponse));
    }

    @GetMapping("/entities/{entityType}/{entityId}/history")
    @PreAuthorize("hasRole('AUDITOR') or hasRole('ADMIN')")
    @Operation(summary = "Get complete history for an entity")
    public ResponseEntity<List<AuditEventResponse>> getEntityHistory(
        @Parameter(description = "Entity type") @PathVariable String entityType,
        @Parameter(description = "Entity ID") @PathVariable String entityId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        Instant s = start != null ? start : Instant.EPOCH;
        Instant e = end != null ? end : Instant.now();
        List<AuditEvent> events = auditService.getEntityHistoryRaw(entityType, entityId, s, e);
        return ResponseEntity.ok(events.stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/events/{id}/verify")
    @PreAuthorize("hasRole('AUDITOR') or hasRole('ADMIN')")
    @Operation(summary = "Verify event integrity (tamper check)")
    public ResponseEntity<Boolean> verifyEvent(
        @Parameter(description = "Event UUID") @PathVariable UUID id
    ) {
        boolean valid = auditService.verifyEventIntegrity(id);
        return ResponseEntity.ok(valid);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('AUDITOR') or hasRole('ADMIN')")
    @Operation(summary = "Get audit statistics for a period")
    public ResponseEntity<AuditStatisticsResponse> getStatistics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        long total = auditService.countEventsInPeriod(start, end);
        List<Object[]> typeStats = auditService.getEventTypeStatistics(start, end);

        List<AuditStatisticsResponse.EventTypeCount> breakdown = typeStats.stream()
            .map(row -> AuditStatisticsResponse.EventTypeCount.builder()
                .eventType((String) row[0])
                .count(((Number) row[1]).longValue())
                .build())
            .toList();

        return ResponseEntity.ok(AuditStatisticsResponse.builder()
            .totalEvents(total)
            .periodStart(start)
            .periodEnd(end)
            .eventTypeBreakdown(breakdown)
            .build());
    }

    @GetMapping("/entities/{entityType}/{entityId}/latest")
    @PreAuthorize("hasRole('AUDITOR') or hasRole('ADMIN')")
    @Operation(summary = "Get latest event for an entity")
    public ResponseEntity<AuditEventResponse> getLatestEvent(
        @PathVariable String entityType,
        @PathVariable String entityId
    ) {
        AuditEvent event = auditService.getLatestEvent(entityType, entityId);
        return event != null
            ? ResponseEntity.ok(mapper.toResponse(event))
            : ResponseEntity.notFound().build();
    }
}