package com.ibank.fee.repository;

import com.ibank.fee.model.FeeConfig;
import com.ibank.fee.model.FeeType;
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
public interface FeeConfigRepository extends JpaRepository<FeeConfig, java.util.UUID> {

    List<FeeConfig> findByFeeType(FeeType feeType);

    List<FeeConfig> findByFeeTypeAndActiveTrue(FeeType feeType);

    List<FeeConfig> findByProductCode(String productCode);

    List<FeeConfig> findByChannel(String channel);

    @Query("SELECT f FROM FeeConfig f WHERE f.feeType = :feeType AND f.active = true " +
           "AND f.validFrom <= :date AND (f.validTo IS NULL OR f.validTo >= :date) " +
           "AND (:productCode IS NULL OR f.productCode = :productCode OR f.productCode IS NULL) " +
           "AND (:channel IS NULL OR f.channel = :channel OR f.channel IS NULL) " +
           "AND (:customerSegment IS NULL OR f.customerSegment = :customerSegment OR f.customerSegment IS NULL) " +
           "ORDER BY f.priority DESC, f.percentageRate DESC")
    List<FeeConfig> findApplicableConfigs(
            @Param("feeType") FeeType feeType,
            @Param("date") LocalDate date,
            @Param("productCode") String productCode,
            @Param("channel") String channel,
            @Param("customerSegment") String customerSegment
    );

    Optional<FeeConfig> findBestMatch(
            @Param("feeType") FeeType feeType,
            @Param("date") LocalDate date,
            @Param("productCode") String productCode,
            @Param("channel") String channel,
            @Param("customerSegment") String customerSegment
    );

    List<FeeConfig> findByValidFromBetween(LocalDate start, LocalDate end);
}