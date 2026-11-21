package com.ibank.antifraud.repository;

import com.ibank.antifraud.model.FraudRule;
import com.ibank.antifraud.model.FraudRuleType;
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
public interface FraudRuleRepository extends JpaRepository<FraudRule, UUID> {

    List<FraudRule> findByRuleType(FraudRuleType ruleType);

    List<FraudRule> findByActiveTrue();

    List<FraudRule> findByActiveTrueOrderByPriorityAsc();

    @Query("SELECT r FROM FraudRule r WHERE r.active = true " +
           "AND r.validFrom <= :date AND (r.validTo IS NULL OR r.validTo >= :date) " +
           "AND (:channel IS NULL OR r.channel = :channel OR r.channel IS NULL) " +
           "AND (:productCode IS NULL OR r.productCode = :productCode OR r.productCode IS NULL) " +
           "AND (:customerSegment IS NULL OR r.customerSegment = :customerSegment OR r.customerSegment IS NULL) " +
           "ORDER BY r.priority ASC")
    List<FraudRule> findApplicableRules(
            @Param("date") LocalDate date,
            @Param("channel") String channel,
            @Param("productCode") String productCode,
            @Param("customerSegment") String customerSegment
    );

    List<FraudRule> findByCreatedBy(UUID createdBy);

    Optional<FraudRule> findByName(String name);
}