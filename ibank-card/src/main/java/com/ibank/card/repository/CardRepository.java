package com.ibank.card.repository;

import com.ibank.card.model.Card;
import com.ibank.card.model.CardStatus;
import com.ibank.card.model.CardType;
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
public interface CardRepository extends JpaRepository<Card, UUID> {

    Optional<Card> findByNumberHash(String numberHash);

    List<Card> findByAccountId(UUID accountId);

    Page<Card> findByAccountId(UUID accountId, Pageable pageable);

    List<Card> findByAccountIdAndStatus(UUID accountId, CardStatus status);

    List<Card> findByType(CardType type);

    List<Card> findByStatus(CardStatus status);

    List<Card> findByExpiryDateBefore(LocalDate date);

    List<Card> findByExpiryDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT c FROM Card c WHERE c.accountId = :accountId AND c.type = :type AND c.status = :status")
    Optional<Card> findActiveByAccountAndType(@Param("accountId") UUID accountId, @Param("type") CardType type, @Param("status") CardStatus status);

    boolean existsByNumberHash(String numberHash);

    long countByAccountIdAndStatus(UUID accountId, CardStatus status);
}