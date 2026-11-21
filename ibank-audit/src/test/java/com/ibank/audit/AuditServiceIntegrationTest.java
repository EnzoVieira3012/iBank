package com.ibank.audit;

import com.ibank.audit.model.AuditEvent;
import com.ibank.audit.model.AuditEventType;
import com.ibank.audit.repository.AuditEventRepository;
import com.ibank.audit.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditServiceIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEventRepository repository;

    @Test
    void recordEvent_shouldPersistAndReturnEvent() {
        AuditEvent event = auditService.recordEvent(
            AuditEventType.ACCOUNT_CREATED,
            "Account",
            "acc-123",
            "user-456",
            "corr-789",
            null,
            Map.of("initialBalance", 1000),
            Map.of("channel", "mobile"),
            "192.168.1.1",
            "Mozilla/5.0"
        );

        assertThat(event.getId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("ACCOUNT_CREATED");
        assertThat(event.getEntityType()).isEqualTo("Account");
        assertThat(event.getEntityId()).isEqualTo("acc-123");
        assertThat(event.getUserId()).isEqualTo("user-456");
        assertThat(event.getCorrelationId()).isEqualTo("corr-789");
        assertThat(event.getPayload()).containsEntry("initialBalance", 1000);
        assertThat(event.getChecksum()).isNotEmpty();
    }

    @Test
    void getEntityHistory_shouldReturnEventsInDescOrder() {
        auditService.recordEvent(AuditEventType.ACCOUNT_CREATED, "Account", "acc-1", "user-1", "c1", null, Map.of(), Map.of(), null, null);
        auditService.recordEvent(AuditEventType.ACCOUNT_UPDATED, "Account", "acc-1", "user-1", "c2", null, Map.of(), Map.of(), null, null);
        auditService.recordEvent(AuditEventType.ACCOUNT_FROZEN, "Account", "acc-1", "user-1", "c3", null, Map.of(), Map.of(), null, null);

        var page = auditService.getEntityHistory("Account", "acc-1", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getEventType()).isEqualTo("ACCOUNT_FROZEN");
        assertThat(page.getContent().get(1).getEventType()).isEqualTo("ACCOUNT_UPDATED");
        assertThat(page.getContent().get(2).getEventType()).isEqualTo("ACCOUNT_CREATED");
    }

    @Test
    void getUserActivity_shouldReturnUserEvents() {
        auditService.recordEvent(AuditEventType.ACCOUNT_CREATED, "Account", "acc-1", "user-1", "c1", null, Map.of(), Map.of(), null, null);
        auditService.recordEvent(AuditEventType.TRANSFER_INITIATED, "Transfer", "txn-1", "user-1", "c2", null, Map.of(), Map.of(), null, null);
        auditService.recordEvent(AuditEventType.ACCOUNT_CREATED, "Account", "acc-2", "user-2", "c3", null, Map.of(), Map.of(), null, null);

        var page = auditService.getUserActivity("user-1", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(e -> e.getUserId().equals("user-1"));
    }

    @Test
    void getCorrelationTrail_shouldReturnChainedEvents() {
        auditService.recordEvent(AuditEventType.TRANSFER_INITIATED, "Transfer", "txn-1", "user-1", "corr-1", null, Map.of(), Map.of(), null, null);
        auditService.recordEvent(AuditEventType.TRANSFER_COMPLETED, "Transfer", "txn-1", "user-1", "corr-1", "event-1", Map.of(), Map.of(), null, null);

        var page = auditService.getCorrelationTrail("corr-1", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(e -> e.getCorrelationId().equals("corr-1"));
    }

    @Test
    void verifyEventIntegrity_shouldDetectTampering() {
        AuditEvent event = auditService.recordEvent(
            AuditEventType.ACCOUNT_CREATED, "Account", "acc-1", "user-1", "c1", null, Map.of("amount", 100), Map.of(), null, null
        );

        // Verify original
        assertThat(auditService.verifyEventIntegrity(event.getId())).isTrue();

        // Tamper directly in DB
        event.setPayload(Map.of("amount", 999));
        repository.saveAndFlush(event);

        // Should fail verification
        assertThat(auditService.verifyEventIntegrity(event.getId())).isFalse();
    }

    @Test
    void getLatestEvent_shouldReturnMostRecent() {
        auditService.recordEvent(AuditEventType.ACCOUNT_CREATED, "Account", "acc-1", "user-1", "c1", null, Map.of(), Map.of(), null, null);
        AuditEvent latest = auditService.recordEvent(AuditEventType.ACCOUNT_UPDATED, "Account", "acc-1", "user-1", "c2", null, Map.of(), Map.of(), null, null);

        AuditEvent result = auditService.getLatestEvent("Account", "acc-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(latest.getId());
        assertThat(result.getEventType()).isEqualTo("ACCOUNT_UPDATED");
    }
}