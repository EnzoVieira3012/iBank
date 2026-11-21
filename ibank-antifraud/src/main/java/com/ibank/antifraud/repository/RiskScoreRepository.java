package com.ibank.antifraud.repository;

import com.ibank.antifraud.model.RiskLevel;
import com.ibank.antifraud.model.RiskScore;
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
public interface RiskScoreRepository extends JpaRepository<RiskScore, UUID> {

    List<RiskScore> findByCustomerId(UUID customerId);

    List<RiskScore> findByAccountId(UUID accountId);

    List<RiskScore> findByCardId(UUID cardId);

    List<RiskScore> findByTransactionId(UUID transactionId);

    List<RiskScore> findByRiskLevel(RiskLevel riskLevel);

    List<RiskScore> findByCalculatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT r FROM RiskScore r WHERE r.customerId = :customerId ORDER BY r.calculatedAt DESC")
    List<RiskScore> findLatestByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT AVG(r.score) FROM RiskScore r WHERE r.customerId = :customerId " +
           "AND r.calculatedAt >= :since")
    Double getAverageScore(@Param("customerId") UUID customerId, @Param("since") LocalDateTime since);

    Optional<RiskScore> findLatestByTransactionId(UUID transactionId);
}