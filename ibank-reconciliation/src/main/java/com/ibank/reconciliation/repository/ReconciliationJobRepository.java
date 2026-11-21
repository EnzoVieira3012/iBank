package com.ibank.reconciliation.repository;

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
public interface ReconciliationJobRepository extends JpaRepository<ReconciliationJob, UUID> {

    Page<ReconciliationJob> findByStatus(String status, Pageable pageable);

    Page<ReconciliationJob> findByJobType(String jobType, Pageable pageable);

    Page<ReconciliationJob> findByStartedAtBetween(Instant start, Instant end, Pageable pageable);

    @Query("""
        SELECT j FROM ReconciliationJob j
        WHERE j.jobType = :jobType
        AND j.startedAt BETWEEN :start AND :end
        ORDER BY j.startedAt DESC
        """)
    List<ReconciliationJob> findByTypeAndPeriod(
        @Param("jobType") String jobType,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    Optional<ReconciliationJob> findLatestByJobType(String jobType);

    @Query("""
        SELECT COUNT(j) FROM ReconciliationJob j
        WHERE j.status IN ('RUNNING', 'PENDING')
        """)
    long countActiveJobs();

    @Query("""
        SELECT j.status, COUNT(j) FROM ReconciliationJob j
        WHERE j.startedAt BETWEEN :start AND :end
        GROUP BY j.status
        """)
    List<Object[]> getStatusStats(@Param("start") Instant start, @Param("end") Instant end);
}