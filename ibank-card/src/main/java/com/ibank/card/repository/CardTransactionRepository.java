package com.ibank.card.repository;

import com.ibank.card.model.CardTransaction;
import com.ibank.card.model.TransactionStatus;
import com.ibank.card.model.TransactionType;
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
public interface CardTransactionRepository extends JpaRepository<CardTransaction, UUID> {

    Optional<CardTransaction> findByIdempotencyKey(String idempotencyKey);

    List<CardTransaction> findByCardId(UUID cardId);

    Page<CardTransaction> findByCardId(UUID cardId, Pageable pageable);

    List<CardTransaction> findByCardIdAndStatus(UUID cardId, TransactionStatus status);

    List<CardTransaction> findByCardIdAndType(UUID cardId, TransactionType type);

    Page<CardTransaction> findByCardIdAndType(UUID cardId, TransactionType type, Pageable pageable);

    List<CardTransaction> findByCardIdAndCreatedAtBetween(UUID cardId, LocalDateTime start, LocalDateTime end);

    Page<CardTransaction> findByCardIdAndCreatedAtBetween(UUID cardId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    List<CardTransaction> findByOriginalTransactionId(UUID originalTransactionId);

    List<CardTransaction> findByAuthorizationCode(String authorizationCode);

    @Query("SELECT SUM(t.amount) FROM CardTransaction t WHERE t.cardId = :cardId AND t.status IN (:statuses) AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumByCardIdAndStatusesAndDateRange(
            @Param("cardId") UUID cardId,
            @Param("statuses") List<TransactionStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(t) FROM CardTransaction t WHERE t.cardId = :cardId AND t.status IN (:statuses) AND t.createdAt BETWEEN :start AND :end")
    long countByCardIdAndStatusesAndDateRange(
            @Param("cardId") UUID cardId,
            @Param("statuses") List<TransactionStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    boolean existsByIdempotencyKey(String idempotencyKey);
}