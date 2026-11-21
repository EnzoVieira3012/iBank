package com.ibank.limit.repository;

import com.ibank.limit.model.LimitType;
import com.ibank.limit.model.LimitUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.jpa.repository.LockModeType.PESSIMISTIC_WRITE;

@Repository
public interface LimitUsageRepository extends JpaRepository<LimitUsage, UUID> {

    Optional<LimitUsage> findByCustomerIdAndLimitTypeAndPeriodStartAndPeriodEnd(
            UUID customerId, LimitType limitType, LocalDateTime periodStart, LocalDateTime periodEnd);

    Optional<LimitUsage> findByAccountIdAndLimitTypeAndPeriodStartAndPeriodEnd(
            UUID accountId, LimitType limitType, LocalDateTime periodStart, LocalDateTime periodEnd);

    Optional<LimitUsage> findByCardIdAndLimitTypeAndPeriodStartAndPeriodEnd(
            UUID cardId, LimitType limitType, LocalDateTime periodStart, LocalDateTime periodEnd);

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT u FROM LimitUsage u WHERE u.customerId = :customerId AND u.limitType = :limitType " +
           "AND u.periodStart = :periodStart AND u.periodEnd = :periodEnd")
    Optional<LimitUsage> findForUpdateByCustomerAndTypeAndPeriod(
            @Param("customerId") UUID customerId,
            @Param("limitType") LimitType limitType,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT u FROM LimitUsage u WHERE u.accountId = :accountId AND u.limitType = :limitType " +
           "AND u.periodStart = :periodStart AND u.periodEnd = :periodEnd")
    Optional<LimitUsage> findForUpdateByAccountAndTypeAndPeriod(
            @Param("accountId") UUID accountId,
            @Param("limitType") LimitType limitType,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT u FROM LimitUsage u WHERE u.cardId = :cardId AND u.limitType = :limitType " +
           "AND u.periodStart = :periodStart AND u.periodEnd = :periodEnd")
    Optional<LimitUsage> findForUpdateByCardAndTypeAndPeriod(
            @Param("cardId") UUID cardId,
            @Param("limitType") LimitType limitType,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

    List<LimitUsage> findByCustomerId(UUID customerId);

    List<LimitUsage> findByAccountId(UUID accountId);

    List<LimitUsage> findByCardId(UUID cardId);

    List<LimitUsage> findByLimitType(LimitType limitType);

    List<LimitUsage> findByPeriodStartBetween(LocalDateTime start, LocalDateTime end);

    List<LimitUsage> findByCustomerIdAndPeriodStartBetween(UUID customerId, LocalDateTime start, LocalDateTime end);

    @Query("DELETE FROM LimitUsage u WHERE u.periodEnd < :date")
    int deleteExpiredUsages(@Param("date") LocalDateTime date);
}