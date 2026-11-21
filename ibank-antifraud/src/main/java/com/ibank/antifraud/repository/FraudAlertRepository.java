package com.ibank.antifraud.repository;

import com.ibank.antifraud.model.AlertSeverity;
import com.ibank.antifraud.model.AlertStatus;
import com.ibank.antifraud.model.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    List<FraudAlert> findByCustomerId(UUID customerId);

    List<FraudAlert> findByAccountId(UUID accountId);

    List<FraudAlert> findByCardId(UUID cardId);

    List<FraudAlert> findByTransactionId(UUID transactionId);

    List<FraudAlert> findByStatus(AlertStatus status);

    List<FraudAlert> findBySeverity(AlertSeverity severity);

    List<FraudAlert> findByAssignedTo(UUID assignedTo);

    List<FraudAlert> findByRuleId(UUID ruleId);

    List<FraudAlert> findByCaseId(UUID caseId);

    List<FraudAlert> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM FraudAlert a WHERE a.status IN (:statuses) ORDER BY a.severity DESC, a.createdAt ASC")
    List<FraudAlert> findOpenAlerts(@Param("statuses") List<AlertStatus> statuses);

    @Query("SELECT a FROM FraudAlert a WHERE a.assignedTo = :assignedTo AND a.status IN (:statuses) " +
           "ORDER BY a.severity DESC, a.createdAt ASC")
    List<FraudAlert> findAssignedOpenAlerts(@Param("assignedTo") UUID assignedTo, @Param("statuses") List<AlertStatus> statuses);

    long countByStatusAndCreatedAtAfter(AlertStatus status, LocalDateTime since);
}