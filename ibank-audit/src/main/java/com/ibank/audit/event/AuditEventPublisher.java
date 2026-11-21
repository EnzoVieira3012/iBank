package com.ibank.audit.event;

import com.ibank.audit.model.AuditEvent;
import com.ibank.audit.model.AuditEventType;
import com.ibank.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

    private final AuditService auditService;

    @Async("auditExecutor")
    @EventListener
    public void handleDomainEvent(DomainAuditEvent event) {
        try {
            auditService.recordEvent(
                event.getEventType(),
                event.getEntityType(),
                event.getEntityId(),
                event.getUserId(),
                event.getCorrelationId(),
                event.getCausationId(),
                event.getPayload(),
                event.getMetadata(),
                event.getIpAddress(),
                event.getUserAgent()
            );
        } catch (Exception e) {
            log.error("Failed to record audit event: {}", event, e);
        }
    }

    public void publish(
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
        DomainAuditEvent event = DomainAuditEvent.builder()
            .eventType(eventType)
            .entityType(entityType)
            .entityId(entityId)
            .userId(userId)
            .correlationId(correlationId)
            .causationId(causationId)
            .payload(payload)
            .metadata(metadata)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();
        
        handleDomainEvent(event);
    }

    @lombok.Builder
    @lombok.Value
    public static class DomainAuditEvent {
        AuditEventType eventType;
        String entityType;
        String entityId;
        String userId;
        String correlationId;
        String causationId;
        Map<String, Object> payload;
        Map<String, Object> metadata;
        String ipAddress;
        String userAgent;
    }
}