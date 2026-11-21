package com.ibank.limit.service;

import com.ibank.limit.controller.dto.LimitValidationRequest;
import com.ibank.limit.controller.dto.LimitValidationResponse;
import com.ibank.limit.model.CustomerLimit;
import com.ibank.limit.model.LimitScope;
import com.ibank.limit.model.LimitType;
import com.ibank.limit.model.LimitUsage;
import com.ibank.limit.model.TransactionBlock;
import com.ibank.limit.model.BlockStatus;
import com.ibank.limit.model.BlockType;
import com.ibank.limit.model.BlockScope;
import com.ibank.limit.repository.CustomerLimitRepository;
import com.ibank.limit.repository.LimitUsageRepository;
import com.ibank.limit.repository.TransactionBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitValidationServiceTest {

    @Mock
    private CustomerLimitRepository limitRepository;

    @Mock
    private LimitUsageRepository usageRepository;

    @Mock
    private TransactionBlockRepository blockRepository;

    @InjectMocks
    private LimitValidationService limitValidationService;

    @Test
    void validate_noLimits_shouldAllow() {
        UUID customerId = UUID.randomUUID();
        when(blockRepository.findActiveBlocksForCustomer(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(limitRepository.findApplicableLimits(any(), any(), any(), any()))
                .thenReturn(List.of());

        LimitValidationRequest request = LimitValidationRequest.builder()
                .customerId(customerId.toString())
                .limitType(LimitType.DAILY_TRANSACTION_AMOUNT)
                .requestedAmount(new BigDecimal("1000"))
                .build();

        LimitValidationResponse response = limitValidationService.validate(request);

        assertTrue(response.isAllowed());
        assertEquals(customerId, response.getCustomerId());
    }

    @Test
    void validate_amountLimitNotExceeded_shouldAllow() {
        UUID customerId = UUID.randomUUID();
        CustomerLimit limit = CustomerLimit.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .limitType(LimitType.DAILY_TRANSACTION_AMOUNT)
                .scope(LimitScope.CUSTOMER)
                .limitValue(new BigDecimal("5000"))
                .warningThreshold(new BigDecimal("80"))
                .blockOnExceed(true)
                .validFrom(LocalDate.now().minusDays(1))
                .active(true)
                .build();

        LimitUsage usage = LimitUsage.builder()
                .customerId(customerId)
                .limitType(LimitType.DAILY_TRANSACTION_AMOUNT)
                .amount(new BigDecimal("1000"))
                .count(5L)
                .build();

        when(blockRepository.findActiveBlocksForCustomer(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(limitRepository.findApplicableLimits(any(), any(), any(), any()))
                .thenReturn(List.of(limit));
        when(usageRepository.findForUpdateByCustomerAndTypeAndPeriod(any(), any(), any(), any()))
                .thenReturn(Optional.of(usage));

        LimitValidationRequest request = LimitValidationRequest.builder()
                .customerId(customerId.toString())
                .limitType(LimitType.DAILY_TRANSACTION_AMOUNT)
                .requestedAmount(new BigDecimal("2000"))
                .build();

        LimitValidationResponse response = limitValidationService.validate(request);

        assertTrue(response.isAllowed());
        assertEquals(new BigDecimal("5000"), response.getLimitValue());
        assertEquals(new BigDecimal("1000"), response.getCurrentUsage());
    }

    @Test
    void validate_amountLimitExceeded_shouldBlock() {
        UUID customerId = UUID.randomUUID();
        CustomerLimit limit = CustomerLimit.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .limitType(LimitType.DAILY_TRANSACTION_AMOUNT)
                .scope(LimitScope.CUSTOMER)
                .limitValue(new BigDecimal("5000"))
                .warningThreshold(new BigDecimal("80"))
                .blockOnExceed(true)
                .validFrom(LocalDate.now().minusDays(1))
                .active(true)
                .build();

        LimitUsage usage = LimitUsage.builder()
                .customerId(customerId)
                .limitType(LimitType.DAILY_TRANSACTION_AMOUNT)
                .amount(new BigDecimal("4500"))
                .count(10L)
                .build();

        when(blockRepository.findActiveBlocksForCustomer(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(limitRepository.findApplicableLimits(any(), any(), any(), any()))
                .thenReturn(List.of(limit));
        when(usageRepository.findForUpdateByCustomerAndTypeAndPeriod(any(), any(), any(), any()))
                .thenReturn(Optional.of(usage));

        LimitValidationRequest request = LimitValidationRequest.builder()
                .customerId(customerId.toString())
                .limitType(LimitType.DAILY_TRANSACTION_AMOUNT)
                .requestedAmount(new BigDecimal("1000"))
                .build();

        LimitValidationResponse response = limitValidationService.validate(request);

        assertFalse(response.isAllowed());
        assertTrue(response.isBlockTriggered());
        assertNotNull(response.getBlockMessage());
    }

    @Test
    void validate_countLimitExceeded_shouldBlock() {
        UUID customerId = UUID.randomUUID();
        CustomerLimit limit = CustomerLimit.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .limitType(LimitType.DAILY_TRANSACTION_COUNT)
                .scope(LimitScope.CUSTOMER)
                .limitValue(new BigDecimal("100"))
                .blockOnExceed(true)
                .validFrom(LocalDate.now().minusDays(1))
                .active(true)
                .build();

        LimitUsage usage = LimitUsage.builder()
                .customerId(customerId)
                .limitType(LimitType.DAILY_TRANSACTION_COUNT)
                .count(100L)
                .build();

        when(blockRepository.findActiveBlocksForCustomer(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(limitRepository.findApplicableLimits(any(), any(), any(), any()))
                .thenReturn(List.of(limit));
        when(usageRepository.findForUpdateByCustomerAndTypeAndPeriod(any(), any(), any(), any()))
                .thenReturn(Optional.of(usage));

        LimitValidationRequest request = LimitValidationRequest.builder()
                .customerId(customerId.toString())
                .limitType(LimitType.DAILY_TRANSACTION_COUNT)
                .build();

        LimitValidationResponse response = limitValidationService.validate(request);

        assertFalse(response.isAllowed());
        assertTrue(response.isBlockTriggered());
    }

    @Test
    void validate_warningThreshold_shouldTriggerWarning() {
        UUID customerId = UUID.randomUUID();
        CustomerLimit limit = CustomerLimit.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .limitType(LimitType.DAILY_TRANSACTION_AMOUNT)
                .scope(LimitScope.CUSTOMER)
                .limitValue(new BigDecimal("5000"))
                .warningThreshold(new BigDecimal("80"))
                .blockOnExceed(true)
                .validFrom(LocalDate.now().minusDays(1))
                .active(true)
                .build();

        LimitUsage usage = LimitUsage.builder()
                .customerId(customerId)
                .limitType(LimitType.DAILY_TRANSACTION_AMOUNT)
                .amount(new BigDecimal("4000"))
                .count(5L)
                .build();

        when(blockRepository.findActiveBlocksForCustomer(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(limitRepository.findApplicableLimits(any(), any(), any(), any()))
                .thenReturn(List.of(limit));
        when(usageRepository.findForUpdateByCustomerAndTypeAndPeriod(any(), any(), any(), any()))
                .thenReturn(Optional.of(usage));

        LimitValidationRequest request = LimitValidationRequest.builder()
                .customerId(customerId.toString())
                .limitType(LimitType.DAILY_TRANSACTION_AMOUNT)
                .requestedAmount(new BigDecimal("500"))
                .build();

        LimitValidationResponse response = limitValidationService.validate(request);

        assertTrue(response.isAllowed());
        assertTrue(response.isWarningTriggered());
        assertNotNull(response.getWarningMessage());
    }

    @Test
    void validate_activeBlock_shouldBlock() {
        UUID customerId = UUID.randomUUID();
        TransactionBlock block = TransactionBlock.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .blockType(BlockType.VELOCITY)
                .scope(BlockScope.CUSTOMER)
                .status(BlockStatus.ACTIVE)
                .reason("Velocity limit exceeded")
                .startsAt(LocalDateTime.now().minusHours(1))
                .build();

        when(blockRepository.findActiveBlocksForCustomer(any(), any(), any(), any()))
                .thenReturn(List.of(block));
        when(limitRepository.findApplicableLimits(any(), any(), any(), any()))
                .thenReturn(List.of());

        LimitValidationRequest request = LimitValidationRequest.builder()
                .customerId(customerId.toString())
                .limitType(LimitType.DAILY_TRANSACTION_AMOUNT)
                .requestedAmount(new BigDecimal("100"))
                .build();

        LimitValidationResponse response = limitValidationService.validate(request);

        assertFalse(response.isAllowed());
        assertTrue(response.isBlockTriggered());
        assertNotNull(response.getBlockMessage());
    }
}