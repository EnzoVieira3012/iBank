package com.ibank.audit;

import com.ibank.audit.model.AuditEvent;
import com.ibank.audit.model.AuditEventType;
import com.ibank.audit.service.ChecksumService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ChecksumServiceTest {

    @Autowired
    private ChecksumService checksumService;

    @Test
    void computeChecksum_shouldBeDeterministic() {
        String entityType = "Account";
        String entityId = "123";
        String eventType = "ACCOUNT_CREATED";
        String payload = "{\"amount\":100}";
        String metadata = "{\"source\":\"api\"}";
        Instant occurredAt = Instant.parse("2024-01-01T10:00:00Z");

        String checksum1 = checksumService.computeChecksum(entityType, entityId, eventType, payload, metadata, occurredAt);
        String checksum2 = checksumService.computeChecksum(entityType, entityId, eventType, payload, metadata, occurredAt);

        assertThat(checksum1).isEqualTo(checksum2);
        assertThat(checksum1).isNotEmpty();
    }

    @Test
    void computeChecksum_shouldDifferForDifferentInputs() {
        Instant occurredAt = Instant.parse("2024-01-01T10:00:00Z");

        String checksum1 = checksumService.computeChecksum("Account", "123", "ACCOUNT_CREATED", "{}", "{}", occurredAt);
        String checksum2 = checksumService.computeChecksum("Account", "456", "ACCOUNT_CREATED", "{}", "{}", occurredAt);

        assertThat(checksum1).isNotEqualTo(checksum2);
    }

    @Test
    void verifyChecksum_shouldReturnTrueForValidEvent() {
        AuditEvent event = AuditEvent.builder()
            .eventType("ACCOUNT_CREATED")
            .entityType("Account")
            .entityId("123")
            .payload(Map.of("amount", 100))
            .metadata(Map.of("source", "api"))
            .occurredAt(Instant.parse("2024-01-01T10:00:00Z"))
            .build();

        String checksum = checksumService.computeChecksum(event);
        event.setChecksum(checksum);

        assertThat(checksumService.verifyChecksum(event)).isTrue();
    }

    @Test
    void verifyChecksum_shouldReturnFalseForTamperedEvent() {
        AuditEvent event = AuditEvent.builder()
            .eventType("ACCOUNT_CREATED")
            .entityType("Account")
            .entityId("123")
            .payload(Map.of("amount", 100))
            .metadata(Map.of("source", "api"))
            .occurredAt(Instant.parse("2024-01-01T10:00:00Z"))
            .build();

        String checksum = checksumService.computeChecksum(event);
        event.setChecksum(checksum);

        // Tamper with payload
        event.setPayload(Map.of("amount", 999));

        assertThat(checksumService.verifyChecksum(event)).isFalse();
    }
}