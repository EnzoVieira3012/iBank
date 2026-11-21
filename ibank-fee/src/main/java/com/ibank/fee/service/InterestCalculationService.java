package com.ibank.fee.service;

import com.ibank.fee.controller.dto.InterestCalculationResponse;
import com.ibank.fee.controller.dto.InterestRateRequest;
import com.ibank.fee.event.InterestAccruedEvent;
import com.ibank.fee.exception.InterestRateNotFoundException;
import com.ibank.fee.mapper.InterestRateMapper;
import com.ibank.fee.model.InterestAccrual;
import com.ibank.fee.model.InterestAccrualStatus;
import com.ibank.fee.model.InterestIndex;
import com.ibank.fee.model.InterestRate;
import com.ibank.fee.repository.InterestAccrualRepository;
import com.ibank.fee.repository.InterestRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestCalculationService {

    private final InterestRateRepository interestRateRepository;
    private final InterestAccrualRepository interestAccrualRepository;
    private final InterestRateMapper interestRateMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final int SCALE = 8;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Transactional(readOnly = true)
    public InterestCalculationResponse calculateSimpleInterest(InterestIndex indexType, BigDecimal principal,
                                                              Integer days, String productCode, LocalDate calculationDate) {
        Optional<InterestRate> rateOpt = interestRateRepository.findLatestRate(indexType, calculationDate, productCode);

        if (rateOpt.isEmpty()) {
            throw new InterestRateNotFoundException("No interest rate found for index: " + indexType + " on date: " + calculationDate);
        }

        InterestRate rate = rateOpt.get();
        BigDecimal effectiveRate = rate.getEffectiveRate() != null ? rate.getEffectiveRate() : rate.getRateValue();
        if (rate.getSpread() != null) {
            effectiveRate = effectiveRate.add(rate.getSpread());
        }

        BigDecimal interestAmount = principal
                .multiply(effectiveRate)
                .divide(HUNDRED, SCALE, ROUNDING)
                .multiply(BigDecimal.valueOf(days))
                .divide(DAYS_IN_YEAR, SCALE, ROUNDING);

        return InterestCalculationResponse.builder()
                .indexType(indexType)
                .baseRate(rate.getRateValue())
                .spread(rate.getSpread())
                .effectiveRate(effectiveRate)
                .principal(principal)
                .days(days)
                .interestAmount(interestAmount.setScale(4, ROUNDING))
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(interestAmount.setScale(4, ROUNDING))
                .calculationDate(calculationDate)
                .calculationMethod("SIMPLE")
                .build();
    }

    @Transactional(readOnly = true)
    public InterestCalculationResponse calculateCompoundInterest(InterestIndex indexType, BigDecimal principal,
                                                                Integer days, String productCode, LocalDate calculationDate,
                                                                Integer compoundingFrequency) {
        Optional<InterestRate> rateOpt = interestRateRepository.findLatestRate(indexType, calculationDate, productCode);

        if (rateOpt.isEmpty()) {
            throw new InterestRateNotFoundException("No interest rate found for index: " + indexType + " on date: " + calculationDate);
        }

        InterestRate rate = rateOpt.get();
        BigDecimal effectiveRate = rate.getEffectiveRate() != null ? rate.getEffectiveRate() : rate.getRateValue();
        if (rate.getSpread() != null) {
            effectiveRate = effectiveRate.add(rate.getSpread());
        }

        BigDecimal dailyRate = effectiveRate.divide(HUNDRED, SCALE, ROUNDING).divide(DAYS_IN_YEAR, SCALE, ROUNDING);
        BigDecimal factor = BigDecimal.ONE.add(dailyRate).pow(days);
        BigDecimal finalAmount = principal.multiply(factor);
        BigDecimal interestAmount = finalAmount.subtract(principal);

        return InterestCalculationResponse.builder()
                .indexType(indexType)
                .baseRate(rate.getRateValue())
                .spread(rate.getSpread())
                .effectiveRate(effectiveRate)
                .principal(principal)
                .days(days)
                .interestAmount(interestAmount.setScale(4, ROUNDING))
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(interestAmount.setScale(4, ROUNDING))
                .calculationDate(calculationDate)
                .calculationMethod("COMPOUND_DAILY")
                .build();
    }

    @Transactional
    public InterestAccrual accrueInterest(UUID accountId, UUID cardId, UUID loanId, BigDecimal principal,
                                         InterestIndex indexType, String productCode, LocalDate accrualDate, Integer days) {
        Optional<InterestRate> rateOpt = interestRateRepository.findLatestRate(indexType, accrualDate, productCode);

        if (rateOpt.isEmpty()) {
            throw new InterestRateNotFoundException("No interest rate found for index: " + indexType + " on date: " + accrualDate);
        }

        InterestRate rate = rateOpt.get();
        BigDecimal effectiveRate = rate.getEffectiveRate() != null ? rate.getEffectiveRate() : rate.getRateValue();
        if (rate.getSpread() != null) {
            effectiveRate = effectiveRate.add(rate.getSpread());
        }

        BigDecimal dailyRate = effectiveRate.divide(HUNDRED, SCALE, ROUNDING).divide(DAYS_IN_YEAR, SCALE, ROUNDING);
        BigDecimal interestAmount = principal.multiply(dailyRate).multiply(BigDecimal.valueOf(days)).setScale(4, ROUNDING);

        InterestAccrual accrual = InterestAccrual.builder()
                .accountId(accountId)
                .cardId(cardId)
                .loanId(loanId)
                .accrualDate(accrualDate)
                .principalBalance(principal)
                .rateApplied(effectiveRate)
                .days(days)
                .interestAmount(interestAmount)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(interestAmount)
                .status(InterestAccrualStatus.ACCRUED)
                .calculationDetails("{\"method\":\"SIMPLE_DAILY\",\"index\":\"" + indexType + "\",\"rate\":" + effectiveRate + "}")
                .build();

        InterestAccrual saved = interestAccrualRepository.save(accrual);

        eventPublisher.publishEvent(new InterestAccruedEvent(
                this, saved.getId(), accountId, cardId, loanId,
                principal, effectiveRate, days, interestAmount, BigDecimal.ZERO, interestAmount
        ));

        log.info("Interest accrued: {} for account {}", saved.getId(), accountId);
        return saved;
    }

    @Transactional
    public void capitalizeAccruals(UUID accountId, LocalDate upToDate) {
        List<InterestAccrual> accruals = interestAccrualRepository.findByAccountIdAndAccrualDateBetween(
                accountId, LocalDate.MIN, upToDate);

        for (InterestAccrual accrual : accruals) {
            if (accrual.getStatus() == InterestAccrualStatus.ACCRUED) {
                accrual.setStatus(InterestAccrualStatus.CAPITALIZED);
                accrual.setCapitalizedAt(java.time.LocalDateTime.now());
                interestAccrualRepository.save(accrual);
            }
        }
    }

    @Transactional
    public void payAccruals(UUID loanId) {
        List<InterestAccrual> accruals = interestAccrualRepository.findByLoanId(loanId);
        for (InterestAccrual accrual : accruals) {
            if (accrual.getStatus() == InterestAccrualStatus.ACCRUED || accrual.getStatus() == InterestAccrualStatus.CAPITALIZED) {
                accrual.setStatus(InterestAccrualStatus.PAID);
                accrual.setPaidAt(java.time.LocalDateTime.now());
                interestAccrualRepository.save(accrual);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<InterestAccrual> findAccrualsByAccount(UUID accountId) {
        return interestAccrualRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalAccruedInterest(UUID accountId) {
        return interestAccrualRepository.sumInterestByAccountAndStatuses(accountId, List.of(InterestAccrualStatus.ACCRUED));
    }

    @Transactional(readOnly = true)
    public InterestRate getById(UUID id) {
        return interestRateRepository.findById(id)
                .orElseThrow(() -> new InterestRateNotFoundException("Interest rate not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<InterestRate> findAll() {
        return interestRateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<InterestRate> findByIndexType(InterestIndex indexType) {
        return interestRateRepository.findByIndexType(indexType);
    }

    @Transactional(readOnly = true)
    public List<InterestRate> findByProductCode(String productCode) {
        return interestRateRepository.findByProductCode(productCode);
    }

    @Transactional(readOnly = true)
    public List<InterestRate> findByIndexTypeAndDateRange(InterestIndex indexType, LocalDate start, LocalDate end) {
        return interestRateRepository.findByIndexTypeAndDateRange(indexType, start, end);
    }

    @Transactional(readOnly = true)
    public InterestRate getLatestRate(InterestIndex indexType, LocalDate date, String productCode) {
        return interestRateRepository.findLatestRate(indexType, date, productCode)
                .orElseThrow(() -> new InterestRateNotFoundException(
                        "No rate found for index: " + indexType + " on date: " + date));
    }

    @Transactional
    public InterestRate upsertRate(InterestRateRequest request) {
        Optional<InterestRate> existing = interestRateRepository.findByIndexTypeAndRateDateAndProductCode(
                request.getIndexType(), request.getRateDate(), request.getProductCode());

        InterestRate rate;
        if (existing.isPresent()) {
            rate = existing.get();
            rate.setRateValue(request.getRateValue());
            rate.setSpread(request.getSpread());
            rate.setSource(request.getSource());
            rate.setMetadata(request.getMetadata());
        } else {
            rate = interestRateMapper.toEntity(request);
        }

        BigDecimal effectiveRate = rate.getRateValue();
        if (rate.getSpread() != null) {
            effectiveRate = effectiveRate.add(rate.getSpread());
        }
        rate.setEffectiveRate(effectiveRate);
        rate.setActive(true);

        return interestRateRepository.save(rate);
    }
}