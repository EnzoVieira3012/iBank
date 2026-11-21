package com.ibank.antifraud.repository;

import com.ibank.antifraud.model.CaseStatus;
import com.ibank.antifraud.model.FraudCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, UUID> {

    Optional<FraudCase> findByCaseNumber(String caseNumber);

    List<FraudCase> findByCustomerId(UUID customerId);

    List<FraudCase> findByStatus(CaseStatus status);

    List<FraudCase> findByAssignedTo(UUID assignedTo);

    List<FraudCase> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<FraudCase> findBySarFiledTrue();

    @Query("SELECT c FROM FraudCase c WHERE c.status IN (:statuses) ORDER BY c.createdAt ASC")
    List<FraudCase> findOpenCases(@Param("statuses") List<CaseStatus> statuses);

    @Query("SELECT c FROM FraudCase c WHERE c.assignedTo = :assignedTo AND c.status IN (:statuses) " +
           "ORDER BY c.createdAt ASC")
    List<FraudCase> findAssignedOpenCases(@Param("assignedTo") UUID assignedTo, @Param("statuses") List<CaseStatus> statuses);

    @Query("SELECT COUNT(c) FROM FraudCase c WHERE c.status = 'SAR_FILED' AND c.sarFiledAt BETWEEN :start AND :end")
    long countSarFiledInPeriod(@Param("start") LocalDate start, @Param("end") LocalDate end);
}