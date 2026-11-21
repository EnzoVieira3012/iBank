package com.ibank.audit.service;

import com.ibank.audit.model.AuditEvent;
import com.ibank.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditExportService {

    private final AuditEventRepository repository;
    private final AuditService auditService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Transactional(readOnly = true)
    public byte[] exportEventsJson(String entityType, String entityId, Instant start, Instant end) throws IOException {
        List<AuditEvent> events = auditService.getEntityHistoryRaw(entityType, entityId, start, end);
        return writeJson(events);
    }

    @Transactional(readOnly = true)
    public byte[] exportEventsJsonByPeriod(Instant start, Instant end, Pageable pageable) throws IOException {
        Page<AuditEvent> page = auditService.getEventsByPeriod(start, end, pageable);
        return writeJson(page.getContent());
    }

    @Transactional(readOnly = true)
    public byte[] exportEventsJsonGzipped(String entityType, String entityId, Instant start, Instant end) throws IOException {
        byte[] json = exportEventsJson(entityType, entityId, start, end);
        return gzip(json);
    }

    @Transactional(readOnly = true)
    public byte[] exportEventsCsv(String entityType, String entityId, Instant start, Instant end) throws IOException {
        List<AuditEvent> events = auditService.getEntityHistoryRaw(entityType, entityId, start, end);
        return writeCsv(events);
    }

    @Transactional(readOnly = true)
    public byte[] exportAllEventsCsv(Instant start, Instant end, Pageable pageable) throws IOException {
        Page<AuditEvent> page = auditService.getEventsByPeriod(start, end, pageable);
        return writeCsv(page.getContent());
    }

    private byte[] writeJson(List<AuditEvent> events) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(baos, events);
            return baos.toByteArray();
        }
    }

    private byte[] writeCsv(List<AuditEvent> events) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,event_type,entity_type,entity_id,user_id,correlation_id,causation_id,occurred_at,checksum\n");
        for (AuditEvent e : events) {
            sb.append(e.getId()).append(',')
              .append(escapeCsv(e.getEventType())).append(',')
              .append(escapeCsv(e.getEntityType())).append(',')
              .append(escapeCsv(e.getEntityId())).append(',')
              .append(escapeCsv(e.getUserId())).append(',')
              .append(escapeCsv(e.getCorrelationId())).append(',')
              .append(escapeCsv(e.getCausationId())).append(',')
              .append(e.getOccurredAt()).append(',')
              .append(escapeCsv(e.getChecksum())).append('\n');
        }
        return sb.toString().getBytes();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private byte[] gzip(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
            gzos.finish();
            return baos.toByteArray();
        }
    }
}