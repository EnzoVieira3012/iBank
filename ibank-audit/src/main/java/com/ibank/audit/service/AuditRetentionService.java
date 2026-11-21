package com.ibank.audit.service;

import com.ibank.audit.model.AuditEvent;
import com.ibank.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditRetentionService {

    private final AuditEventRepository repository;

    @Scheduled(cron = "${ibank.audit.retention.cron:0 0 2 * * ?}")
    @Transactional
    public void archiveOldEvents() {
        Instant cutoff = Instant.now().minusSeconds(365 * 24 * 60 * 60); // 1 year default
        int batchSize = 1000;

        log.info("Starting audit event archival for events before {}", cutoff);

        int totalArchived = 0;
        List<UUID> batch;
        do {
            batch = repository.findIdsByOccurredAtBefore(cutoff, batchSize);
            if (!batch.isEmpty()) {
                repository.archiveEvents(batch);
                totalArchived += batch.size();
                log.debug("Archived batch of {} events", batch.size());
            }
        } while (!batch.isEmpty());

        log.info("Audit archival completed. Total events archived: {}", totalArchived);
    }

    @Transactional(readOnly = true)
    public long countEventsOlderThan(Instant cutoff) {
        return repository.countByOccurredAtBefore(cutoff);
    }

    @Transactional
    public int deleteArchivedEvents(Instant cutoff) {
        return repository.deleteArchivedBefore(cutoff);
    }
}