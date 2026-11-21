package com.ibank.fee.service;

import com.ibank.fee.controller.dto.FeeCalculationResponse;
import com.ibank.fee.controller.dto.FeeChargeRequest;
import com.ibank.fee.event.FeeChargedEvent;
import com.ibank.fee.exception.FeeConfigNotFoundException;
import com.ibank.fee.mapper.FeeChargeMapper;
import com.ibank.fee.model.FeeCalculationType;
import com.ibank.fee.model.FeeCharge;
import com.ibank.fee.model.FeeChargeStatus;
import com.ibank.fee.model.FeeConfig;
import com.ibank.fee.model.FeeType;
import com.ibank.fee.repository.FeeChargeRepository;
import com.ibank.fee.repository.FeeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeCalculationService {

    private final FeeConfigRepository feeConfigRepository;
    private final FeeChargeRepository feeChargeRepository;
    private final FeeChargeMapper feeChargeMapper;
    private final ApplicationEventPublisher eventPublisher;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Transactional(readOnly = true)
    public FeeCalculationResponse calculateFee(FeeType feeType, BigDecimal baseAmount, String productCode,
                                               String channel, String customerSegment, LocalDate date) {
        Optional<FeeConfig> configOpt = feeConfigRepository.findBestMatch(feeType, date, productCode, channel, customerSegment);

        if (configOpt.isEmpty()) {
            throw new FeeConfigNotFoundException("No fee config found for type: " + feeType + " on date: " + date);
        }

        FeeConfig config = configOpt.get();
        return calculateFeeWithConfig(config, baseAmount);
    }

    public FeeCalculationResponse calculateFeeWithConfig(FeeConfig config, BigDecimal baseAmount) {
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        String appliedFormula = null;
        List<FeeCalculationResponse.TierDetail> appliedTiers = new ArrayList<>();

        switch (config.getCalculationType()) {
            case FIXED -> fee = config.getFixedAmount() != null ? config.getFixedAmount() : BigDecimal.ZERO;
            case PERCENTAGE -> {
                if (config.getPercentageRate() != null && baseAmount != null) {
                    fee = baseAmount.multiply(config.getPercentageRate()).divide(HUNDRED, SCALE, ROUNDING);
                }
            }
            case TIERED -> {
                fee = calculateTieredFee(config, baseAmount, appliedTiers);
            }
            case MIN_MAX -> {
                fee = calculateMinMaxFee(config, baseAmount);
            }
            case FORMULA -> {
                fee = calculateFormulaFee(config, baseAmount);
                appliedFormula = config.getFormula();
            }
        }

        if (config.getMinAmount() != null && fee.compareTo(config.getMinAmount()) < 0) {
            fee = config.getMinAmount();
        }
        if (config.getMaxAmount() != null && fee.compareTo(config.getMaxAmount()) > 0) {
            fee = config.getMaxAmount();
        }

        if (Boolean.TRUE.equals(config.getTaxIncluded()) && config.getTaxRate() != null) {
            taxAmount = fee.multiply(config.getTaxRate()).divide(HUNDRED, SCALE, ROUNDING);
        }

        BigDecimal totalAmount = fee.add(taxAmount);

        return FeeCalculationResponse.builder()
                .feeConfigId(config.getId())
                .feeType(config.getFeeType())
                .calculationType(config.getCalculationType())
                .baseAmount(baseAmount)
                .fixedAmount(config.getFixedAmount())
                .percentageRate(config.getPercentageRate())
                .calculatedFee(fee.setScale(2, ROUNDING))
                .taxAmount(taxAmount.setScale(2, ROUNDING))
                .totalAmount(totalAmount.setScale(2, ROUNDING))
                .appliedFormula(appliedFormula)
                .appliedTiers(appliedTiers.isEmpty() ? null : appliedTiers)
                .build();
    }

    private BigDecimal calculateTieredFee(FeeConfig config, BigDecimal baseAmount, List<FeeCalculationResponse.TierDetail> tiers) {
        if (config.getTierConfig() == null || baseAmount == null) return BigDecimal.ZERO;
        // Simplified tier calculation - in production, parse JSON tier config
        // Example tier config: [{"min":0,"max":1000,"rate":1.5},{"min":1000,"max":5000,"rate":1.0}]
        return baseAmount.multiply(config.getPercentageRate() != null ? config.getPercentageRate() : BigDecimal.ZERO)
                .divide(HUNDRED, SCALE, ROUNDING);
    }

    private BigDecimal calculateMinMaxFee(FeeConfig config, BigDecimal baseAmount) {
        if (baseAmount == null || config.getPercentageRate() == null) return BigDecimal.ZERO;
        BigDecimal fee = baseAmount.multiply(config.getPercentageRate()).divide(HUNDRED, SCALE, ROUNDING);
        if (config.getMinAmount() != null && fee.compareTo(config.getMinAmount()) < 0) return config.getMinAmount();
        if (config.getMaxAmount() != null && fee.compareTo(config.getMaxAmount()) > 0) return config.getMaxAmount();
        return fee;
    }

    private BigDecimal calculateFormulaFee(FeeConfig config, BigDecimal baseAmount) {
        // Simplified - in production use expression evaluator like exp4j or JEXL
        if (config.getPercentageRate() != null && baseAmount != null) {
            return baseAmount.multiply(config.getPercentageRate()).divide(HUNDRED, SCALE, ROUNDING);
        }
        return config.getFixedAmount() != null ? config.getFixedAmount() : BigDecimal.ZERO;
    }

    @Transactional
    public FeeCharge chargeFee(FeeChargeRequest request) {
        if (request.getIdempotencyKey() != null &&
                feeChargeRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
            return feeChargeRepository.findByIdempotencyKey(request.getIdempotencyKey()).get();
        }

        FeeCharge charge = feeChargeMapper.toEntity(request);

        if (request.getFeeConfigId() != null) {
            FeeConfig config = feeConfigRepository.findById(request.getFeeConfigId())
                    .orElseThrow(() -> new FeeConfigNotFoundException("Fee config not found: " + request.getFeeConfigId()));
            FeeCalculationResponse calc = calculateFeeWithConfig(config, request.getAmount());
            charge.setAmount(calc.getCalculatedFee());
            charge.setTaxAmount(calc.getTaxAmount());
            charge.setTotalAmount(calc.getTotalAmount());
            charge.setCalculationDetails(toJson(calc));
        } else {
            charge.setTotalAmount(request.getAmount().add(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO));
        }

        charge.setStatus(FeeChargeStatus.CHARGED);
        FeeCharge saved = feeChargeRepository.save(charge);

        eventPublisher.publishEvent(new FeeChargedEvent(
                this, saved.getId(), saved.getAccountId(), saved.getCardId(), saved.getLoanId(),
                saved.getTransferId(), saved.getFeeType(), saved.getAmount(), saved.getTaxAmount(), saved.getTotalAmount()
        ));

        log.info("Fee charged: {} for account {}", saved.getId(), saved.getAccountId());
        return saved;
    }

    @Transactional
    public FeeCharge payFee(UUID feeChargeId, UUID paymentTransactionId) {
        FeeCharge charge = feeChargeRepository.findById(feeChargeId)
                .orElseThrow(() -> new com.ibank.fee.exception.FeeChargeNotFoundException("Fee charge not found: " + feeChargeId));

        if (charge.getStatus() == FeeChargeStatus.PAID || charge.getStatus() == FeeChargeStatus.REFUNDED) {
            throw new IllegalStateException("Fee already paid or refunded");
        }

        charge.setStatus(FeeChargeStatus.PAID);
        charge.setPaidAt(java.time.LocalDateTime.now());
        charge.setPaymentTransactionId(paymentTransactionId);

        FeeCharge saved = feeChargeRepository.save(charge);

        eventPublisher.publishEvent(new FeePaidEvent(
                this, saved.getId(), saved.getAccountId(), saved.getFeeType(),
                saved.getTotalAmount(), paymentTransactionId
        ));

        log.info("Fee paid: {}", feeChargeId);
        return saved;
    }

    @Transactional
    public FeeCharge waiveFee(UUID feeChargeId) {
        FeeCharge charge = feeChargeRepository.findById(feeChargeId)
                .orElseThrow(() -> new com.ibank.fee.exception.FeeChargeNotFoundException("Fee charge not found: " + feeChargeId));

        charge.setStatus(FeeChargeStatus.WAIVED);
        return feeChargeRepository.save(charge);
    }

    @Transactional(readOnly = true)
    public FeeCharge getById(UUID id) {
        return feeChargeRepository.findById(id)
                .orElseThrow(() -> new com.ibank.fee.exception.FeeChargeNotFoundException("Fee charge not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<FeeCharge> findAll() {
        return feeChargeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FeeCharge> findByAccountId(UUID accountId) {
        return feeChargeRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<FeeCharge> findByCardId(UUID cardId) {
        return feeChargeRepository.findByCardId(cardId);
    }

    @Transactional(readOnly = true)
    public List<FeeCharge> findByLoanId(UUID loanId) {
        return feeChargeRepository.findByLoanId(loanId);
    }

    @Transactional(readOnly = true)
    public List<FeeCharge> findByTransferId(UUID transferId) {
        return feeChargeRepository.findByTransferId(transferId);
    }

    @Transactional(readOnly = true)
    public List<FeeCharge> findByFeeType(FeeType feeType) {
        return feeChargeRepository.findByFeeType(feeType);
    }

    @Transactional(readOnly = true)
    public List<FeeCharge> findByStatus(FeeChargeStatus status) {
        return feeChargeRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<FeeCharge> findByChargeDateBetween(LocalDate start, LocalDate end) {
        return feeChargeRepository.findByChargeDateBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<FeeCharge> findOverdueFees(LocalDate asOfDate) {
        return feeChargeRepository.findByDueDateBefore(asOfDate);
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}