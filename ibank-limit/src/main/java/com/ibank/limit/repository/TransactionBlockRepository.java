package com.ibank.limit.repository;

import com.ibank.limit.model.BlockScope;
import com.ibank.limit.model.BlockStatus;
import com.ibank.limit.model.BlockType;
import com.ibank.limit.model.TransactionBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionBlockRepository extends JpaRepository<TransactionBlock, UUID> {

    List<TransactionBlock> findByCustomerId(UUID customerId);

    List<TransactionBlock> findByAccountId(UUID accountId);

    List<TransactionBlock> findByCardId(UUID cardId);

    List<TransactionBlock> findByBlockType(BlockType blockType);

    List<TransactionBlock> findByStatus(BlockStatus status);

    List<TransactionBlock> findByScope(BlockScope scope);

    @Query("SELECT b FROM TransactionBlock b WHERE b.customerId = :customerId " +
           "AND b.status = 'ACTIVE' " +
           "AND b.startsAt <= :now " +
           "AND (b.expiresAt IS NULL OR b.expiresAt >= :now) " +
           "AND (:accountId IS NULL OR b.accountId = :accountId OR b.accountId IS NULL) " +
           "AND (:cardId IS NULL OR b.cardId = :cardId OR b.cardId IS NULL) " +
           "ORDER BY b.blockType DESC, b.startsAt DESC")
    List<TransactionBlock> findActiveBlocksForCustomer(
            @Param("customerId") UUID customerId,
            @Param("now") LocalDateTime now,
            @Param("accountId") UUID accountId,
            @Param("cardId") UUID cardId
    );

    @Query("SELECT b FROM TransactionBlock b WHERE b.accountId = :accountId " +
           "AND b.status = 'ACTIVE' " +
           "AND b.startsAt <= :now " +
           "AND (b.expiresAt IS NULL OR b.expiresAt >= :now) " +
           "ORDER BY b.blockType DESC, b.startsAt DESC")
    List<TransactionBlock> findActiveBlocksForAccount(
            @Param("accountId") UUID accountId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT b FROM TransactionBlock b WHERE b.cardId = :cardId " +
           "AND b.status = 'ACTIVE' " +
           "AND b.startsAt <= :now " +
           "AND (b.expiresAt IS NULL OR b.expiresAt >= :now) " +
           "ORDER BY b.blockType DESC, b.startsAt DESC")
    List<TransactionBlock> findActiveBlocksForCard(
            @Param("cardId") UUID cardId,
            @Param("now") LocalDateTime now
    );

    List<TransactionBlock> findByCreatedBy(UUID createdBy);

    List<TransactionBlock> findByExpiresAtBefore(LocalDateTime now);

    @Query("SELECT b FROM TransactionBlock b WHERE b.status = 'ACTIVE' " +
           "AND b.expiresAt IS NOT NULL AND b.expiresAt < :now")
    List<TransactionBlock> findExpiredActiveBlocks(@Param("now") LocalDateTime now);
}