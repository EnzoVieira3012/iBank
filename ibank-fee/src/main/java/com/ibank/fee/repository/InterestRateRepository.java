package com.ibank.fee.repository;

import com.ibank.fee.model.InterestIndex;
import com.ibank.fee.model.InterestRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterestRateRepository extends JpaRepository<InterestRate, java.util.UUID> {

    List<InterestRate> findByIndexType(InterestIndex indexType);

    List<InterestRate> findByIndexTypeAndActiveTrue(InterestIndex indexType);

    List<InterestRate> findByProductCode(String productCode);

    @Query("SELECT r FROM InterestRate r WHERE r.indexType = :indexType AND r.active = true " +
           "AND r.rateDate <= :date " +
           "AND (:productCode IS NULL OR r.productCode = :productCode OR r.productCode IS NULL) " +
           "ORDER BY r.rateDate DESC")
    List<InterestRate> findLatestRates(
            @Param("indexType") InterestIndex indexType,
            @Param("date") LocalDate date,
            @Param("productCode") String productCode
    );

    Optional<InterestRate> findLatestRate(
            @Param("indexType") InterestIndex indexType,
            @Param("date") LocalDate date,
            @Param("productCode") String productCode
    );

    @Query("SELECT r FROM InterestRate r WHERE r.rateDate BETWEEN :start AND :end " +
           "AND r.indexType = :indexType ORDER BY r.rateDate ASC")
    List<InterestRate> findByIndexTypeAndDateRange(
            @Param("indexType") InterestIndex indexType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    boolean existsByIndexTypeAndRateDateAndProductCode(InterestIndex indexType, LocalDate rateDate, String productCode);
}