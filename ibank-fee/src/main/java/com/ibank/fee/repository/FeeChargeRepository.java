package com.ibank.fee.repository;

import com.ibank.fee.model.FeeCharge;
import com.ibank.fee.model.FeeChargeStatus;
import com.ibank.fee.model.FeeType;
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
public interface FeeChargeRepository extends JpaRepository<FeeCharge, UUID> {

    Optional<FeeCharge> findByIdempotencyKey(String idempotencyKey);

    List<FeeCharge> findByAccountId(UUID accountId);

    Page<FeeCharge> findByAccountId(UUID accountId, Pageable pageable);

    List<FeeCharge> findByCardId(UUID cardId);

    List<FeeCharge> findByLoanId(UUID loanId);

    List<FeeCharge> findByTransferId(UUID transferId);

    List<FeeCharge> findByFeeConfigId(UUID feeConfigId);

    List<FeeCharge> findByFeeType(FeeType feeType);

    List<FeeCharge> findByStatus(FeeChargeStatus status);

    List<FeeCharge> findByAccountIdAndStatus(UUID accountId, FeeChargeStatus status);

    List<FeeCharge> findByChargeDateBetween(LocalDate start, LocalDate end);

    List<FeeCharge> findByDueDateBefore(LocalDate date);

    List<FeeCharge> findByDueDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT SUM(fc.totalAmount) FROM FeeCharge fc WHERE fc.accountId = :accountId AND fc.status IN (:statuses)")
    BigDecimal sumByAccountIdAndStatuses(@Param("accountId") UUID accountId, @Param("statuses") List<FeeChargeStatus> statuses);

    @Query("SELECT COUNT(fc) FROM FeeCharge fc WHERE fc.accountId = :accountId AND fc.status = :status AND fc.dueDate < :date")
    long countOverdueByAccount(@Param("accountId") UUID accountId, @Param("status") FeeChargeStatus status, @Param("date") LocalDate date);
}