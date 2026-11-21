package com.ibank.fee.service;

import com.ibank.fee.controller.dto.FeeCalculationResponse;
import com.ibank.fee.model.FeeCalculationType;
import com.ibank.fee.model.FeeConfig;
import com.ibank.fee.model.FeePeriod;
import com.ibank.fee.model.FeeType;
import com.ibank.fee.repository.FeeConfigRepository;
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
class FeeCalculationServiceTest {

    @Mock
    private FeeConfigRepository feeConfigRepository;

    @InjectMocks
    private FeeCalculationService feeCalculationService;

    @Test
    void calculateFixedFee_shouldReturnFixedAmount() {
        FeeConfig config = FeeConfig.builder()
                .id(UUID.randomUUID())
                .feeType(FeeType.ACCOUNT_MAINTENANCE)
                .calculationType(FeeCalculationType.FIXED)
                .fixedAmount(new BigDecimal("25.00"))
                .period(FeePeriod.MONTHLY)
                .validFrom(LocalDate.now().minusMonths(1))
                .active(true)
                .build();

        when(feeConfigRepository.findBestMatch(any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(config));

        FeeCalculationResponse response = feeCalculationService.calculateFee(
                FeeType.ACCOUNT_MAINTENANCE, new BigDecimal("1000"), null, null, null, LocalDate.now());

        assertEquals(new BigDecimal("25.00"), response.getCalculatedFee());
        assertEquals(new BigDecimal("25.00"), response.getTotalAmount());
    }

    @Test
    void calculatePercentageFee_shouldReturnPercentageOfBase() {
        FeeConfig config = FeeConfig.builder()
                .id(UUID.randomUUID())
                .feeType(FeeType.TRANSFER_TED)
                .calculationType(FeeCalculationType.PERCENTAGE)
                .percentageRate(new BigDecimal("1.5"))
                .period(FeePeriod.PER_TRANSACTION)
                .validFrom(LocalDate.now().minusMonths(1))
                .active(true)
                .build();

        when(feeConfigRepository.findBestMatch(any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(config));

        FeeCalculationResponse response = feeCalculationService.calculateFee(
                FeeType.TRANSFER_TED, new BigDecimal("1000"), null, null, null, LocalDate.now());

        assertEquals(new BigDecimal("15.00"), response.getCalculatedFee());
        assertEquals(new BigDecimal("15.00"), response.getTotalAmount());
    }

    @Test
    void calculatePercentageFeeWithTax_shouldIncludeTax() {
        FeeConfig config = FeeConfig.builder()
                .id(UUID.randomUUID())
                .feeType(FeeType.CARD_ANNUAL)
                .calculationType(FeeCalculationType.PERCENTAGE)
                .percentageRate(new BigDecimal("2.0"))
                .period(FeePeriod.ANNUAL)
                .taxIncluded(true)
                .taxRate(new BigDecimal("5.0"))
                .validFrom(LocalDate.now().minusMonths(1))
                .active(true)
                .build();

        when(feeConfigRepository.findBestMatch(any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(config));

        FeeCalculationResponse response = feeCalculationService.calculateFee(
                FeeType.CARD_ANNUAL, new BigDecimal("500"), null, null, null, LocalDate.now());

        assertEquals(new BigDecimal("10.00"), response.getCalculatedFee());
        assertEquals(new BigDecimal("0.50"), response.getTaxAmount());
        assertEquals(new BigDecimal("10.50"), response.getTotalAmount());
    }

    @Test
    void calculatePercentageFeeWithMinMax_shouldRespectLimits() {
        FeeConfig config = FeeConfig.builder()
                .id(UUID.randomUUID())
                .feeType(FeeType.TRANSFER_PIX)
                .calculationType(FeeCalculationType.MIN_MAX)
                .percentageRate(new BigDecimal("1.0"))
                .minAmount(new BigDecimal("1.00"))
                .maxAmount(new BigDecimal("10.00"))
                .period(FeePeriod.PER_TRANSACTION)
                .validFrom(LocalDate.now().minusMonths(1))
                .active(true)
                .build();

        when(feeConfigRepository.findBestMatch(any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(config));

        // Below min
        FeeCalculationResponse response1 = feeCalculationService.calculateFee(
                FeeType.TRANSFER_PIX, new BigDecimal("50"), null, null, null, LocalDate.now());
        assertEquals(new BigDecimal("1.00"), response1.getCalculatedFee());

        // Above max
        FeeCalculationResponse response2 = feeCalculationService.calculateFee(
                FeeType.TRANSFER_PIX, new BigDecimal("2000"), null, null, null, LocalDate.now());
        assertEquals(new BigDecimal("10.00"), response2.getCalculatedFee());

        // Within range
        FeeCalculationResponse response3 = feeCalculationService.calculateFee(
                FeeType.TRANSFER_PIX, new BigDecimal("500"), null, null, null, LocalDate.now());
        assertEquals(new BigDecimal("5.00"), response3.getCalculatedFee());
    }
}