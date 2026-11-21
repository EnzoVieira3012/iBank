package com.ibank.limit.repository;

import com.ibank.limit.model.CustomerLimit;
import com.ibank.limit.model.LimitScope;
import com.ibank.limit.model.LimitType;
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
public interface CustomerLimitRepository extends JpaRepository<CustomerLimit, UUID> {

    List<CustomerLimit> findByCustomerId(UUID customerId);

    List<CustomerLimit> findByCustomerIdAndActiveTrue(UUID customerId);

    List<CustomerLimit> findByAccountId(UUID accountId);

    List<CustomerLimit> findByCardId(UUID cardId);

    List<CustomerLimit> findByLimitType(LimitType limitType);

    List<CustomerLimit> findByScope(LimitScope scope);

    @Query("SELECT l FROM CustomerLimit l WHERE l.customerId = :customerId AND l.active = true " +
           "AND l.validFrom <= :date AND (l.validTo IS NULL OR l.validTo >= :date) " +
           "AND (:accountId IS NULL OR l.accountId = :accountId OR l.accountId IS NULL) " +
           "AND (:cardId IS NULL OR l.cardId = :cardId OR l.cardId IS NULL) " +
           "ORDER BY l.scope ASC, l.limitType ASC")
    List<CustomerLimit> findApplicableLimits(
            @Param("customerId") UUID customerId,
            @Param("date") LocalDate date,
            @Param("accountId") UUID accountId,
            @Param("cardId") UUID cardId
    );

    @Query("SELECT l FROM CustomerLimit l WHERE l.customerId = :customerId AND l.limitType = :limitType " +
           "AND l.active = true AND l.validFrom <= :date AND (l.validTo IS NULL OR l.validTo >= :date) " +
           "AND (:accountId IS NULL OR l.accountId = :accountId OR l.accountId IS NULL) " +
           "AND (:cardId IS NULL OR l.cardId = :cardId OR l.cardId IS NULL) " +
           "ORDER BY l.scope ASC")
    List<CustomerLimit> findByCustomerAndType(
            @Param("customerId") UUID customerId,
            @Param("limitType") LimitType limitType,
            @Param("date") LocalDate date,
            @Param("accountId") UUID accountId,
            @Param("cardId") UUID cardId
    );

    List<CustomerLimit> findByValidFromBetween(LocalDate start, LocalDate end);

    long countByCustomerIdAndActiveTrue(UUID customerId);
}