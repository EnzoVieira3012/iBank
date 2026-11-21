package com.ibank.pix.repository;

import com.ibank.pix.model.QRCode;
import com.ibank.pix.model.QRCodeType;
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
public interface QRCodeRepository extends JpaRepository<QRCode, UUID> {

    Optional<QRCode> findByEmv(String emv);

    List<QRCode> findByAccountId(UUID accountId);

    List<QRCode> findByPixKeyId(UUID pixKeyId);

    List<QRCode> findByAccountIdAndQrCodeType(UUID accountId, QRCodeType qrCodeType);

    Page<QRCode> findByAccountId(UUID accountId, Pageable pageable);

    Optional<QRCode> findByTransactionId(UUID transactionId);

    @Query("SELECT q FROM QRCode q WHERE " +
           "(:accountId IS NULL OR q.accountId = :accountId) AND " +
           "(:qrCodeType IS NULL OR q.qrCodeType = :qrCodeType) AND " +
           "(:pixKeyId IS NULL OR q.pixKeyId = :pixKeyId)")
    Page<QRCode> findWithFilters(
            @Param("accountId") UUID accountId,
            @Param("qrCodeType") QRCodeType qrCodeType,
            @Param("pixKeyId") UUID pixKeyId,
            Pageable pageable);

    long countByAccountIdAndUsedAtIsNull(UUID accountId);

    @Query("DELETE FROM QRCode q WHERE q.expiresAt IS NOT NULL AND q.expiresAt < CURRENT_TIMESTAMP AND q.usedAt IS NULL")
    int deleteExpiredUnused();
}