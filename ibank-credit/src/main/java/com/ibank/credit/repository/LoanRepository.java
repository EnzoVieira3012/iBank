package com.ibank.credit.repository;

import com.ibank.credit.model.Loan;
import com.ibank.credit.model.LoanStatus;
import com.ibank.credit.model.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    Optional<Loan> findByContractNumber(String contractNumber);

    List<Loan> findByAccountId(UUID accountId);

    List<Loan> findByAccountIdAndStatus(UUID accountId, LoanStatus status);

    List<Loan> findByStatus(LoanStatus status);

    Page<Loan> findByAccountId(UUID accountId, Pageable pageable);

    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE " +
           "(:accountId IS NULL OR l.accountId = :accountId) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:loanType IS NULL OR l.loanType = :loanType) AND " +
           "(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR l.createdAt <= :endDate)")
    Page<Loan> findWithFilters(
            @Param("accountId") UUID accountId,
            @Param("status") LoanStatus status,
            @Param("loanType") LoanType loanType,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE', 'RENEGOTIATED') AND l.accountId = :accountId")
    List<Loan> findActiveLoansByAccount(UUID accountId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.accountId = :accountId AND l.status NOT IN ('SETTLED', 'CANCELLED', 'REJECTED')")
    long countActiveLoansByAccount(UUID accountId);

    @Query("SELECT SUM(l.outstandingBalance.amount) FROM Loan l WHERE l.accountId = :accountId AND l.status IN ('ACTIVE', 'OVERDUE', 'RENEGOTIATED')")
    java.math.BigDecimal sumOutstandingByAccount(UUID accountId);
}