package com.ibank.fee.service;

import com.ibank.fee.controller.dto.InterestCalculationResponse;
import com.ibank.fee.model.InterestIndex;
import com.ibank.fee.model.InterestRate;
import com.ibank.fee.repository.InterestRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterestCalculationServiceTest {

    @Mock
    private InterestRateRepository interestRateRepository;

    @InjectMocks
    private InterestCalculationService interestCalculationService;

    @Test
    void calculateSimpleInterest_shouldReturnCorrectAmount() {
        InterestRate rate = InterestRate.builder()
                .id(UUID.randomUUID())
                .indexType(InterestIndex.CDI)
                .rateDate(LocalDate.now())
                .rateValue(new BigDecimal("13.75"))
                .spread(BigDecimal.ZERO)
                .effectiveRate(new BigDecimal("13.75"))
                .active(true)
                .build();

        when(interestRateRepository.findLatestRate(eq(InterestIndex.CDI), any(), any()))
                .thenReturn(Optional.of(rate));

        InterestCalculationResponse response = interestCalculationService.calculateSimpleInterest(
                InterestIndex.CDI, new BigDecimal("10000"), 30, null, LocalDate.now());

        // 10000 * 13.75% / 100 * 30 / 365 = 113.01
        assertEquals(new BigDecimal("113.01"), response.getInterestAmount().setScale(2));
        assertEquals("SIMPLE", response.getCalculationMethod());
    }

    @Test
    void calculateSimpleInterestWithSpread_shouldAddSpread() {
        InterestRate rate = InterestRate.builder()
                .id(UUID.randomUUID())
                .indexType(InterestIndex.SELIC)
                .rateDate(LocalDate.now())
                .rateValue(new BigDecimal("13.75"))
                .spread(new BigDecimal("2.5"))
                .effectiveRate(new BigDecimal("16.25"))
                .active(true)
                .build();

        when(interestRateRepository.findLatestRate(eq(InterestIndex.SELIC), any(), any()))
                .thenReturn(Optional.of(rate));

        InterestCalculationResponse response = interestCalculationService.calculateSimpleInterest(
                InterestIndex.SELIC, new BigDecimal("10000"), 30, null, LocalDate.now());

        // 10000 * 16.25% / 100 * 30 / 365 = 133.56
        assertEquals(new BigDecimal("133.56"), response.getInterestAmount().setScale(2));
        assertEquals(new BigDecimal("16.25"), response.getEffectiveRate());
    }

    @Test
    void calculateCompoundInterest_shouldReturnHigherAmount() {
        InterestRate rate = InterestRate.builder()
                .id(UUID.randomUUID())
                .indexType(InterestIndex.CDI)
                .rateDate(LocalDate.now())
                .rateValue(new BigDecimal("13.75"))
                .spread(BigDecimal.ZERO)
                .effectiveRate(new BigDecimal("13.75"))
                .active(true)
                .build();

        when(interestRateRepository.findLatestRate(eq(InterestIndex.CDI), any(), any()))
                .thenReturn(Optional.of(rate));

        InterestCalculationResponse simple = interestCalculationService.calculateSimpleInterest(
                InterestIndex.CDI, new BigDecimal("10000"), 365, null, LocalDate.now());

        InterestCalculationResponse compound = interestCalculationService.calculateCompoundInterest(
                InterestIndex.CDI, new BigDecimal("10000"), 365, null, LocalDate.now(), 365);

        assertTrue(compound.getInterestAmount().compareTo(simple.getInterestAmount()) > 0);
        assertEquals("COMPOUND_DAILY", compound.getCalculationMethod());
    }

    @Test
    void calculateSimpleInterest_noRateFound_shouldThrowException() {
        when(interestRateRepository.findLatestRate(eq(InterestIndex.TJLP), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(com.ibank.fee.exception.InterestRateNotFoundException.class, () ->
                interestCalculationService.calculateSimpleInterest(
                        InterestIndex.TJLP, new BigDecimal("10000"), 30, null, LocalDate.now()));
    }
}