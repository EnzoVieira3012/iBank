package com.ibank.pix.repository;

import com.ibank.pix.model.PixTransaction;
import com.ibank.pix.model.PixTransactionStatus;
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
public interface PixTransactionRepository extends JpaRepository<PixTransaction, UUID> {

    Optional<PixTransaction> findByEndToEndId(String endToEndId);

    List<PixTransaction> findByPayerAccountId(UUID accountId);

    List<PixTransaction> findByPayeeAccountId(UUID accountId);

    List<PixTransaction> findByPayerAccountIdAndStatus(UUID accountId, PixTransactionStatus status);

    List<PixTransaction> findByPayeeAccountIdAndStatus(UUID accountId, PixTransactionStatus status);

    Page<PixTransaction> findByPayerAccountId(UUID accountId, Pageable pageable);

    Page<PixTransaction> findByPayeeAccountId(UUID accountId, Pageable pageable);

    @Query("SELECT t FROM PixTransaction t WHERE " +
           "(:accountId IS NULL OR t.payerAccountId = :accountId OR t.payeeAccountId = :accountId) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:startDate IS NULL OR t.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR t.createdAt <= :endDate) AND " +
           "(:keyId IS NULL OR t.payerKeyId = :keyId OR t.payeeKeyId = :keyId)")
    Page<PixTransaction> findWithFilters(
            @Param("accountId") UUID accountId,
            @Param("status") PixTransactionStatus status,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("keyId") UUID keyId,
            Pageable pageable);

    @Query("SELECT COUNT(t) FROM PixTransaction t WHERE t.payerAccountId = :accountId AND t.status = :status")
    long countByPayerAccountIdAndStatus(UUID accountId, PixTransactionStatus status);

    @Query("SELECT COUNT(t) FROM PixTransaction t WHERE t.payeeAccountId = :accountId AND t.status = :status")
    long countByPayeeAccountIdAndStatus(UUID accountId, PixTransactionStatus status);

    @Query("SELECT SUM(t.amount.amount) FROM PixTransaction t WHERE t.payeeAccountId = :accountId AND t.status = 'COMPLETED' AND t.createdAt >= :startDate")
    java.math.BigDecimal sumReceivedByAccountSince(UUID accountId, OffsetDateTime startDate);

    @Query("SELECT SUM(t.amount.amount) FROM PixTransaction t WHERE t.payerAccountId = :accountId AND t.status = 'COMPLETED' AND t.createdAt >= :startDate")
    java.math.BigDecimal sumSentByAccountSince(UUID accountId, OffsetDateTime startDate);

    Optional<PixTransaction> findByRefundId(UUID refundId);
}