package com.ibank.credit.repository;

import com.ibank.credit.model.LoanSimulation;
import com.ibank.credit.model.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanSimulationRepository extends JpaRepository<LoanSimulation, UUID> {

    List<LoanSimulation> findByAccountId(UUID accountId);

    Page<LoanSimulation> findByAccountId(UUID accountId, Pageable pageable);

    @Query("SELECT s FROM LoanSimulation s WHERE " +
           "(:accountId IS NULL OR s.accountId = :accountId) AND " +
           "(:loanType IS NULL OR s.loanType = :loanType) AND " +
           "(:startDate IS NULL OR s.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR s.createdAt <= :endDate)")
    Page<LoanSimulation> findWithFilters(
            @Param("accountId") UUID accountId,
            @Param("loanType") LoanType loanType,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);
}