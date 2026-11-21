package com.ibank.audit.repository;

import com.ibank.audit.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    Page<AuditEvent> findByUserId(String userId, Pageable pageable);

    Page<AuditEvent> findByCorrelationId(String correlationId, Pageable pageable);

    Page<AuditEvent> findByOccurredAtBetween(Instant start, Instant end, Pageable pageable);

    Page<AuditEvent> findByEventTypeIn(List<String> eventTypes, Pageable pageable);

    @Query("""
        SELECT a FROM AuditEvent a
        WHERE a.entityType = :entityType
        AND a.entityId = :entityId
        AND a.occurredAt BETWEEN :start AND :end
        ORDER BY a.occurredAt DESC
        """)
    List<AuditEvent> findEntityHistory(
        @Param("entityType") String entityType,
        @Param("entityId") String entityId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    @Query("""
        SELECT COUNT(a) FROM AuditEvent a
        WHERE a.occurredAt BETWEEN :start AND :end
        """)
    long countByPeriod(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
        SELECT a.eventType, COUNT(a) FROM AuditEvent a
        WHERE a.occurredAt BETWEEN :start AND :end
        GROUP BY a.eventType
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> getEventTypeStats(@Param("start") Instant start, @Param("end") Instant end);

    Optional<AuditEvent> findLatestByEntityTypeAndEntityId(String entityType, String entityId);

    boolean existsByChecksum(String checksum);

    Optional<AuditEvent> findById(UUID id);

    // Archive methods
    @Query("SELECT a.id FROM AuditEvent a WHERE a.occurredAt < :cutoff AND a.archived = false")
    List<UUID> findIdsByOccurredAtBefore(@Param("cutoff") Instant cutoff, int limit);

    @Modifying
    @Transactional
    @Query("UPDATE AuditEvent a SET a.archived = true, a.archivedAt = NOW() WHERE a.id IN :ids")
    void archiveEvents(@Param("ids") List<UUID> ids);

    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.occurredAt < :cutoff")
    long countByOccurredAtBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Transactional
    @Query("DELETE FROM AuditEvent a WHERE a.archived = true AND a.archivedAt < :cutoff")
    int deleteArchivedBefore(@Param("cutoff") Instant cutoff);
}