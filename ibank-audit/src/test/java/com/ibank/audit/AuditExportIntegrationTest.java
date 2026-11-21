package com.ibank.audit;

import com.ibank.audit.model.AuditEventType;
import com.ibank.audit.service.AuditExportService;
import com.ibank.audit.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditExportIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditExportService exportService;

    @Test
    void exportEntityJson_shouldReturnValidJson() throws IOException {
        auditService.recordEvent(AuditEventType.ACCOUNT_CREATED, "Account", "acc-1", "user-1", "c1", null, Map.of("bal", 100), Map.of(), null, null);
        auditService.recordEvent(AuditEventType.ACCOUNT_UPDATED, "Account", "acc-1", "user-1", "c2", null, Map.of("bal", 200), Map.of(), null, null);

        Instant start = Instant.parse("2020-01-01T00:00:00Z");
        Instant end = Instant.now().plusSeconds(3600);

        byte[] json = exportService.exportEventsJson("Account", "acc-1", start, end);

        assertThat(json).isNotEmpty();
        String jsonStr = new String(json);
        assertThat(jsonStr).contains("ACCOUNT_CREATED");
        assertThat(jsonStr).contains("ACCOUNT_UPDATED");
        assertThat(jsonStr).contains("acc-1");
    }

    @Test
    void exportEntityJsonGzipped_shouldReturnCompressed() throws IOException {
        auditService.recordEvent(AuditEventType.ACCOUNT_CREATED, "Account", "acc-1", "user-1", "c1", null, Map.of(), Map.of(), null, null);

        Instant start = Instant.parse("2020-01-01T00:00:00Z");
        Instant end = Instant.now().plusSeconds(3600);

        byte[] gzipped = exportService.exportEventsJsonGzipped("Account", "acc-1", start, end);
        byte[] plain = exportService.exportEventsJson("Account", "acc-1", start, end);

        assertThat(gzipped).isNotEmpty();
        // Gzipped should be smaller for larger payloads
        assertThat(gzipped.length).isLessThanOrEqualTo(plain.length + 20); // overhead for small payload
    }

    @Test
    void exportEntityCsv_shouldReturnValidCsv() throws IOException {
        auditService.recordEvent(AuditEventType.ACCOUNT_CREATED, "Account", "acc-1", "user-1", "c1", null, Map.of(), Map.of(), null, null);

        Instant start = Instant.parse("2020-01-01T00:00:00Z");
        Instant end = Instant.now().plusSeconds(3600);

        byte[] csv = exportService.exportEventsCsv("Account", "acc-1", start, end);

        assertThat(csv).isNotEmpty();
        String csvStr = new String(csv);
        assertThat(csvStr).contains("id,event_type,entity_type");
        assertThat(csvStr).contains("ACCOUNT_CREATED");
        assertThat(csvStr).contains("acc-1");
    }

    @Test
    void exportPeriodCsv_shouldReturnAllEventsInPeriod() throws IOException {
        auditService.recordEvent(AuditEventType.ACCOUNT_CREATED, "Account", "acc-1", "user-1", "c1", null, Map.of(), Map.of(), null, null);
        auditService.recordEvent(AuditEventType.TRANSFER_INITIATED, "Transfer", "txn-1", "user-1", "c2", null, Map.of(), Map.of(), null, null);
        auditService.recordEvent(AuditEventType.CARD_ISSUED, "Card", "card-1", "user-2", "c3", null, Map.of(), Map.of(), null, null);

        Instant start = Instant.parse("2020-01-01T00:00:00Z");
        Instant end = Instant.now().plusSeconds(3600);

        byte[] csv = exportService.exportAllEventsCsv(start, end, PageRequest.of(0, 100));

        assertThat(csv).isNotEmpty();
        String csvStr = new String(csv);
        assertThat(csvStr).contains("ACCOUNT_CREATED");
        assertThat(csvStr).contains("TRANSFER_INITIATED");
        assertThat(csvStr).contains("CARD_ISSUED");
    }
}