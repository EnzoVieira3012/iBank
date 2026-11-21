package com.ibank.audit.service;

import com.ibank.audit.model.AuditEvent;
import com.ibank.audit.model.AuditEventType;
import com.ibank.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditEventRepository repository;
    private final ChecksumService checksumService;

    @Transactional
    public AuditEvent recordEvent(
        AuditEventType eventType,
        String entityType,
        String entityId,
        String userId,
        String correlationId,
        String causationId,
        Map<String, Object> payload,
        Map<String, Object> metadata,
        String ipAddress,
        String userAgent
    ) {
        AuditEvent event = AuditEvent.builder()
            .eventType(eventType.name())
            .entityType(entityType)
            .entityId(entityId)
            .userId(userId)
            .correlationId(correlationId)
            .causationId(causationId)
            .payload(payload)
            .metadata(metadata)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .occurredAt(Instant.now())
            .build();

        String checksum = checksumService.computeChecksum(event);
        event.setChecksum(checksum);

        AuditEvent saved = repository.save(event);
        log.debug("Recorded audit event: {} for {}:{}", eventType, entityType, entityId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getEntityHistory(String entityType, String entityId, Pageable pageable) {
        return repository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getUserActivity(String userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getCorrelationTrail(String correlationId, Pageable pageable) {
        return repository.findByCorrelationId(correlationId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getEventsByPeriod(Instant start, Instant end, Pageable pageable) {
        return repository.findByOccurredAtBetween(start, end, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getEventsByTypes(List<String> eventTypes, Pageable pageable) {
        return repository.findByEventTypeIn(eventTypes, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getEntityHistoryRaw(String entityType, String entityId,
                                                 Instant start, Instant end) {
        return repository.findEntityHistory(entityType, entityId, start, end);
    }

    @Transactional(readOnly = true)
    public AuditEvent getLatestEvent(String entityType, String entityId) {
        return repository.findLatestByEntityTypeAndEntityId(entityType, entityId).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean verifyEventIntegrity(UUID eventId) {
        return repository.findById(eventId)
            .map(checksumService::verifyChecksum)
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public long countEventsInPeriod(Instant start, Instant end) {
        return repository.countByPeriod(start, end);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<AuditEvent> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getEventTypeStatistics(Instant start, Instant end) {
        return repository.getEventTypeStats(start, end);
    }
}