package com.ibank.pix.repository;

import com.ibank.pix.model.PixKey;
import com.ibank.pix.model.PixKeyStatus;
import com.ibank.pix.model.PixKeyType;
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
public interface PixKeyRepository extends JpaRepository<PixKey, UUID> {

    Optional<PixKey> findByKeyValue(String keyValue);

    List<PixKey> findByAccountId(UUID accountId);

    List<PixKey> findByHolderCpf(String cpf);

    List<PixKey> findByAccountIdAndStatus(UUID accountId, PixKeyStatus status);

    List<PixKey> findByKeyType(PixKeyType keyType);

    Page<PixKey> findByAccountId(UUID accountId, Pageable pageable);

    Page<PixKey> findByHolderCpf(String cpf, Pageable pageable);

    @Query("SELECT k FROM PixKey k WHERE " +
           "(:accountId IS NULL OR k.accountId = :accountId) AND " +
           "(:status IS NULL OR k.status = :status) AND " +
           "(:keyType IS NULL OR k.keyType = :keyType) AND " +
           "(:holderCpf IS NULL OR k.holderCpf = :holderCpf)")
    Page<PixKey> findWithFilters(
            @Param("accountId") UUID accountId,
            @Param("status") PixKeyStatus status,
            @Param("keyType") PixKeyType keyType,
            @Param("holderCpf") String holderCpf,
            Pageable pageable);

    boolean existsByKeyValue(String keyValue);

    long countByAccountIdAndStatus(UUID accountId, PixKeyStatus status);

    Optional<PixKey> findByDictRegistrationId(String dictRegistrationId);
}