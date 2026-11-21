package com.ibank.reconciliation.repository;

import com.ibank.reconciliation.model.ReconciliationRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationRuleRepository extends JpaRepository<ReconciliationRule, UUID> {

    List<ReconciliationRule> findByJobTypeAndActiveTrueOrderByPriorityDesc(String jobType);

    List<ReconciliationRule> findByEntityTypeAndActiveTrueOrderByPriorityDesc(String entityType);

    @Query("""
        SELECT r FROM ReconciliationRule r
        WHERE r.jobType = :jobType
        AND r.entityType = :entityType
        AND r.active = true
        ORDER BY r.priority DESC
        """)
    List<ReconciliationRule> findByJobTypeAndEntityType(
        @Param("jobType") String jobType,
        @Param("entityType") String entityType
    );

    Optional<ReconciliationRule> findByName(String name);
}