package com.ibank.credit.repository;

import com.ibank.credit.model.Installment;
import com.ibank.credit.model.InstallmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, UUID> {

    List<Installment> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    List<Installment> findByLoanIdAndStatus(UUID loanId, InstallmentStatus status);

    List<Installment> findByLoanIdAndDueDateBetween(UUID loanId, LocalDate startDate, LocalDate endDate);

    Page<Installment> findByLoanId(UUID loanId, Pageable pageable);

    List<Installment> findByDueDateBeforeAndStatusIn(LocalDate date, List<InstallmentStatus> statuses);

    @Query("SELECT i FROM Installment i WHERE i.loanId = :loanId AND i.status IN ('PENDING', 'PARTIALLY_PAID', 'OVERDUE') ORDER BY i.installmentNumber ASC")
    List<Installment> findOpenInstallmentsByLoan(UUID loanId);

    @Query("SELECT COUNT(i) FROM Installment i WHERE i.loanId = :loanId AND i.status = 'PAID'")
    long countPaidInstallments(UUID loanId);

    @Query("SELECT SUM(i.paidTotal.amount) FROM Installment i WHERE i.loanId = :loanId AND i.status IN ('PAID', 'PARTIALLY_PAID')")
    java.math.BigDecimal sumPaidByLoan(UUID loanId);

    @Query("SELECT SUM(i.totalAmount.amount) FROM Installment i WHERE i.loanId = :loanId AND i.status IN ('PENDING', 'PARTIALLY_PAID', 'OVERDUE')")
    java.math.BigDecimal sumOutstandingByLoan(UUID loanId);
}