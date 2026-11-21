package com.ibank.fee.repository;

import com.ibank.fee.model.InterestAccrual;
import com.ibank.fee.model.InterestAccrualStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InterestAccrualRepository extends JpaRepository<InterestAccrual, UUID> {

    List<InterestAccrual> findByAccountId(UUID accountId);

    Page<InterestAccrual> findByAccountId(UUID accountId, Pageable pageable);

    List<InterestAccrual> findByCardId(UUID cardId);

    List<InterestAccrual> findByLoanId(UUID loanId);

    List<InterestAccrual> findByStatus(InterestAccrualStatus status);

    List<InterestAccrual> findByAccrualDateBetween(LocalDate start, LocalDate end);

    List<InterestAccrual> findByAccountIdAndAccrualDateBetween(UUID accountId, LocalDate start, LocalDate end);

    @Query("SELECT SUM(ia.interestAmount) FROM InterestAccrual ia WHERE ia.accountId = :accountId AND ia.status IN (:statuses)")
    BigDecimal sumInterestByAccountAndStatuses(@Param("accountId") UUID accountId, @Param("statuses") List<InterestAccrualStatus> statuses);

    @Query("SELECT SUM(ia.totalAmount) FROM InterestAccrual ia WHERE ia.loanId = :loanId AND ia.status = 'ACCRUED'")
    BigDecimal sumAccruedByLoan(@Param("loanId") UUID loanId);
}