package com.ibank.reconciliation.repository;

import com.ibank.reconciliation.model.ReconciliationBreak;
import com.ibank.reconciliation.model.ReconciliationJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationBreakRepository extends JpaRepository<ReconciliationBreak, UUID> {

    Page<ReconciliationBreak> findByJob(ReconciliationJob job, Pageable pageable);

    Page<ReconciliationBreak> findByJobId(UUID jobId, Pageable pageable);

    Page<ReconciliationBreak> findByStatus(String status, Pageable pageable);

    Page<ReconciliationBreak> findByBreakType(String breakType, Pageable pageable);

    Page<ReconciliationBreak> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    Page<ReconciliationBreak> findByAssigneeId(String assigneeId, Pageable pageable);

    Page<ReconciliationBreak> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    Page<ReconciliationBreak> findByStatusAndAssigneeId(String status, String assigneeId, Pageable pageable);

    @Query("""
        SELECT b FROM ReconciliationBreak b
        WHERE b.job.id = :jobId
        AND b.status = 'OPEN'
        ORDER BY
            CASE b.priority
                WHEN 'CRITICAL' THEN 0
                WHEN 'HIGH' THEN 1
                WHEN 'MEDIUM' THEN 2
                WHEN 'LOW' THEN 3
            END,
            b.createdAt ASC
        """)
    List<ReconciliationBreak> findOpenBreaksPrioritized(@Param("jobId") UUID jobId);

    @Query("""
        SELECT b FROM ReconciliationBreak b
        WHERE b.dueDate < :now
        AND b.status IN ('OPEN', 'IN_PROGRESS')
        ORDER BY b.dueDate ASC
        """)
    List<ReconciliationBreak> findOverdueBreaks(@Param("now") Instant now);

    @Query("""
        SELECT b.breakType, COUNT(b) FROM ReconciliationBreak b
        WHERE b.job.id = :jobId
        GROUP BY b.breakType
        """)
    List<Object[]> getBreakTypeStatsByJob(@Param("jobId") UUID jobId);

    @Query("""
        SELECT COUNT(b) FROM ReconciliationBreak b
        WHERE b.job.id = :jobId
        AND b.status = 'OPEN'
        """)
    long countOpenBreaksByJob(@Param("jobId") UUID jobId);

    Optional<ReconciliationBreak> findBySourceReference(String sourceReference);
}