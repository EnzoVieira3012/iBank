package com.ibank.transfer.repository;

import com.ibank.transfer.model.Transfer;
import com.ibank.transfer.model.TransferStatus;
import com.ibank.transfer.model.TransferType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    Optional<Transfer> findByEndToEndId(String endToEndId);

    List<Transfer> findByPayerAccountId(UUID payerAccountId);

    List<Transfer> findByPayeeAccountId(UUID payeeAccountId);

    List<Transfer> findByPayerAccountIdAndStatus(UUID payerAccountId, TransferStatus status);

    List<Transfer> findByPayeeAccountIdAndStatus(UUID payeeAccountId, TransferStatus status);

    List<Transfer> findByStatus(TransferStatus status);

    List<Transfer> findByTransferType(TransferType transferType);

    Page<Transfer> findByPayerAccountId(UUID payerAccountId, Pageable pageable);

    Page<Transfer> findByPayeeAccountId(UUID payeeAccountId, Pageable pageable);

    @Query("SELECT t FROM Transfer t WHERE " +
           "(:accountId IS NULL OR t.payerAccountId = :accountId OR t.payeeAccountId = :accountId) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:transferType IS NULL OR t.transferType = :transferType) AND " +
           "(:startDate IS NULL OR t.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR t.createdAt <= :endDate)")
    Page<Transfer> findWithFilters(
            @Param("accountId") UUID accountId,
            @Param("status") TransferStatus status,
            @Param("transferType") TransferType transferType,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);

    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.payerAccountId = :accountId AND t.status IN ('COMPLETED', 'PROCESSING') AND t.createdAt >= :startDate")
    long countCompletedTransfersSince(UUID accountId, OffsetDateTime startDate);

    @Query("SELECT SUM(t.amount.amount) FROM Transfer t WHERE t.payerAccountId = :accountId AND t.status = 'COMPLETED' AND t.createdAt >= :startDate")
    java.math.BigDecimal sumSentByAccountSince(UUID accountId, OffsetDateTime startDate);

    @Query("SELECT SUM(t.amount.amount) FROM Transfer t WHERE t.payeeAccountId = :accountId AND t.status = 'COMPLETED' AND t.createdAt >= :startDate")
    java.math.BigDecimal sumReceivedByAccountSince(UUID accountId, OffsetDateTime startDate);

    @Query("SELECT t FROM Transfer t WHERE t.status = 'SCHEDULED' AND t.scheduledAt <= :now")
    List<Transfer> findScheduledTransfersReadyToExecute(@Param("now") OffsetDateTime now);

    boolean existsByIdempotencyKey(String idempotencyKey);
}