package com.ibank.limit.service;

import com.ibank.limit.controller.dto.LimitValidationRequest;
import com.ibank.limit.controller.dto.LimitValidationResponse;
import com.ibank.limit.event.LimitExceededEvent;
import com.ibank.limit.exception.LimitExceededException;
import com.ibank.limit.exception.TransactionBlockedException;
import com.ibank.limit.model.BlockScope;
import com.ibank.limit.model.BlockStatus;
import com.ibank.limit.model.CustomerLimit;
import com.ibank.limit.model.LimitScope;
import com.ibank.limit.model.LimitType;
import com.ibank.limit.model.LimitUsage;
import com.ibank.limit.model.TransactionBlock;
import com.ibank.limit.repository.CustomerLimitRepository;
import com.ibank.limit.repository.LimitUsageRepository;
import com.ibank.limit.repository.TransactionBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LimitValidationService {

    private final CustomerLimitRepository limitRepository;
    private final LimitUsageRepository usageRepository;
    private final TransactionBlockRepository blockRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Transactional(readOnly = true)
    public LimitValidationResponse validate(LimitValidationRequest request) {
        UUID customerId = UUID.fromString(request.getCustomerId());
        UUID accountId = request.getAccountId() != null ? UUID.fromString(request.getAccountId()) : null;
        UUID cardId = request.getCardId() != null ? UUID.fromString(request.getCardId()) : null;
        LimitType limitType = request.getLimitType();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<TransactionBlock> activeBlocks = blockRepository.findActiveBlocksForCustomer(
                customerId, now, accountId, cardId);

        if (!activeBlocks.isEmpty()) {
            TransactionBlock block = activeBlocks.get(0);
            return buildBlockedResponse(customerId, accountId, cardId, limitType, block, activeBlocks);
        }

        List<CustomerLimit> limits = limitRepository.findApplicableLimits(customerId, today, accountId, cardId);

        if (limits.isEmpty()) {
            return LimitValidationResponse.builder()
                    .allowed(true)
                    .customerId(customerId)
                    .accountId(accountId)
                    .cardId(cardId)
                    .limitType(limitType)
                    .build();
        }

        CustomerLimit mostRestrictive = findMostRestrictiveLimit(limits, limitType, request.getRequestedAmount(), request.getRequestedCount());

        if (mostRestrictive == null) {
            return LimitValidationResponse.builder()
                    .allowed(true)
                    .customerId(customerId)
                    .accountId(accountId)
                    .cardId(cardId)
                    .limitType(limitType)
                    .build();
        }

        LimitUsage usage = getOrCreateUsage(mostRestrictive, customerId, accountId, cardId, now);
        BigDecimal currentAmount = usage.getAmount() != null ? usage.getAmount() : BigDecimal.ZERO;
        Long currentCount = usage.getCount() != null ? usage.getCount() : 0L;

        BigDecimal newAmount = currentAmount.add(request.getRequestedAmount() != null ? request.getRequestedAmount() : BigDecimal.ZERO);
        Long newCount = currentCount + (request.getRequestedCount() != null ? request.getRequestedCount() : 1L);

        BigDecimal limitValue = mostRestrictive.getLimitValue();
        double usagePercentage = 0;

        boolean limitExceeded = false;
        boolean warningTriggered = false;
        String warningMessage = null;
        String blockMessage = null;

        if (isAmountLimit(limitType)) {
            if (limitValue.compareTo(BigDecimal.ZERO) > 0) {
                usagePercentage = newAmount.divide(limitValue, 4, ROUNDING).multiply(HUNDRED).doubleValue();
            }
            if (newAmount.compareTo(limitValue) > 0) {
                limitExceeded = true;
            } else if (mostRestrictive.getWarningThreshold() != null) {
                BigDecimal threshold = limitValue.multiply(mostRestrictive.getWarningThreshold()).divide(HUNDRED, SCALE, ROUNDING);
                if (newAmount.compareTo(threshold) >= 0) {
                    warningTriggered = true;
                    warningMessage = String.format("Warning: %.1f%% of %s limit used", usagePercentage, limitType);
                }
            }
        } else {
            if (limitValue.compareTo(BigDecimal.ZERO) > 0) {
                usagePercentage = (double) newCount / limitValue.longValue() * 100;
            }
            if (newCount > limitValue.longValue()) {
                limitExceeded = true;
            } else if (mostRestrictive.getWarningThreshold() != null) {
                long threshold = (long) (limitValue.longValue() * mostRestrictive.getWarningThreshold().doubleValue() / 100);
                if (newCount >= threshold) {
                    warningTriggered = true;
                    warningMessage = String.format("Warning: %d/%d %s used", newCount, limitValue.longValue(), limitType);
                }
            }
        }

        if (limitExceeded) {
            blockMessage = String.format("%s limit exceeded: %s/%s", limitType,
                    isAmountLimit(limitType) ? newAmount : newCount, limitValue);

            eventPublisher.publishEvent(new LimitExceededEvent(
                    this, customerId, accountId, cardId, limitType, limitValue,
                    isAmountLimit(limitType) ? currentAmount : BigDecimal.valueOf(currentCount),
                    request.getRequestedAmount(), request.getRequestedCount(), mostRestrictive.getScope().name()
            ));

            if (Boolean.TRUE.equals(mostRestrictive.getBlockOnExceed())) {
                throw new LimitExceededException(blockMessage, limitType, mostRestrictive.getScope().name(),
                        isAmountLimit(limitType) ? currentAmount.toString() : String.valueOf(currentCount),
                        limitValue.toString());
            }
        }

        return LimitValidationResponse.builder()
                .allowed(!limitExceeded || !Boolean.TRUE.equals(mostRestrictive.getBlockOnExceed()))
                .customerId(customerId)
                .accountId(accountId)
                .cardId(cardId)
                .limitType(limitType)
                .limitValue(limitValue)
                .currentUsage(isAmountLimit(limitType) ? currentAmount : BigDecimal.valueOf(currentCount))
                .requestedAmount(request.getRequestedAmount())
                .requestedCount(request.getRequestedCount())
                .remainingAmount(isAmountLimit(limitType) ? limitValue.subtract(currentAmount) : null)
                .remainingCount(!isAmountLimit(limitType) ? limitValue.longValue() - currentCount : null)
                .usagePercentage(usagePercentage)
                .warningTriggered(warningTriggered)
                .blockTriggered(limitExceeded && Boolean.TRUE.equals(mostRestrictive.getBlockOnExceed()))
                .warningMessage(warningMessage)
                .blockMessage(blockMessage)
                .activeBlocks(activeBlocks)
                .build();
    }

    @Transactional
    public void recordUsage(UUID customerId, UUID accountId, UUID cardId, LimitType limitType,
                            BigDecimal amount, Long count) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = calculatePeriodStart(limitType, now);
        LocalDateTime periodEnd = calculatePeriodEnd(limitType, now);

        LimitUsage usage = getOrCreateUsageForRecording(customerId, accountId, cardId, limitType, periodStart, periodEnd, now);
        usage.setCount(usage.getCount() + (count != null ? count : 1L));
        if (amount != null) {
            usage.setAmount(usage.getAmount() != null ? usage.getAmount().add(amount) : amount);
        }
        usage.setLastTransactionAt(now);
        usageRepository.save(usage);
    }

    private LimitUsage getOrCreateUsage(CustomerLimit limit, UUID customerId, UUID accountId, UUID cardId, LocalDateTime now) {
        LocalDateTime periodStart = calculatePeriodStart(limit.getLimitType(), now);
        LocalDateTime periodEnd = calculatePeriodEnd(limit.getLimitType(), now);

        return getOrCreateUsageForRecording(customerId, accountId, cardId, limit.getLimitType(), periodStart, periodEnd, now);
    }

    private LimitUsage getOrCreateUsageForRecording(UUID customerId, UUID accountId, UUID cardId,
                                                    LimitType limitType, LocalDateTime periodStart,
                                                    LocalDateTime periodEnd, LocalDateTime now) {
        Optional<LimitUsage> existing;
        if (cardId != null) {
            existing = usageRepository.findForUpdateByCardAndTypeAndPeriod(cardId, limitType, periodStart, periodEnd);
        } else if (accountId != null) {
            existing = usageRepository.findForUpdateByAccountAndTypeAndPeriod(accountId, limitType, periodStart, periodEnd);
        } else {
            existing = usageRepository.findForUpdateByCustomerAndTypeAndPeriod(customerId, limitType, periodStart, periodEnd);
        }

        if (existing.isPresent()) {
            return existing.get();
        }

        LimitUsage usage = LimitUsage.builder()
                .customerId(customerId)
                .accountId(accountId)
                .cardId(cardId)
                .limitType(limitType)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .count(0L)
                .amount(BigDecimal.ZERO)
                .build();

        return usageRepository.save(usage);
    }

    private LocalDateTime calculatePeriodStart(LimitType limitType, LocalDateTime now) {
        return switch (limitType) {
            case DAILY_TRANSACTION_COUNT, DAILY_TRANSACTION_AMOUNT, DAILY_PIX_COUNT, DAILY_PIX_AMOUNT,
                 DAILY_TRANSFER_COUNT, DAILY_TRANSFER_AMOUNT, DAILY_TED_COUNT, DAILY_TED_AMOUNT,
                 DAILY_CARD_PURCHASE_COUNT, DAILY_CARD_PURCHASE_AMOUNT, DAILY_CARD_WITHDRAWAL_COUNT,
                 DAILY_CARD_WITHDRAWAL_AMOUNT, DAILY_CASH_ADVANCE_AMOUNT ->
                now.toLocalDate().atStartOfDay();
            case MONTHLY_TRANSACTION_COUNT, MONTHLY_TRANSACTION_AMOUNT, MONTHLY_PIX_COUNT, MONTHLY_PIX_AMOUNT ->
                now.toLocalDate().withDayOfMonth(1).atStartOfDay();
            default -> now.toLocalDate().atStartOfDay();
        };
    }

    private LocalDateTime calculatePeriodEnd(LimitType limitType, LocalDateTime now) {
        return switch (limitType) {
            case DAILY_TRANSACTION_COUNT, DAILY_TRANSACTION_AMOUNT, DAILY_PIX_COUNT, DAILY_PIX_AMOUNT,
                 DAILY_TRANSFER_COUNT, DAILY_TRANSFER_AMOUNT, DAILY_TED_COUNT, DAILY_TED_AMOUNT,
                 DAILY_CARD_PURCHASE_COUNT, DAILY_CARD_PURCHASE_AMOUNT, DAILY_CARD_WITHDRAWAL_COUNT,
                 DAILY_CARD_WITHDRAWAL_AMOUNT, DAILY_CASH_ADVANCE_AMOUNT ->
                now.toLocalDate().atTime(23, 59, 59);
            case MONTHLY_TRANSACTION_COUNT, MONTHLY_TRANSACTION_AMOUNT, MONTHLY_PIX_COUNT, MONTHLY_PIX_AMOUNT ->
                now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth()).atTime(23, 59, 59);
            default -> now.toLocalDate().atTime(23, 59, 59);
        };
    }

    private CustomerLimit findMostRestrictiveLimit(List<CustomerLimit> limits, LimitType limitType,
                                                   BigDecimal requestedAmount, Long requestedCount) {
        List<CustomerLimit> matching = limits.stream()
                .filter(l -> l.getLimitType() == limitType)
                .toList();

        if (matching.isEmpty()) return null;

        return matching.stream()
                .min((a, b) -> a.getLimitValue().compareTo(b.getLimitValue()))
                .orElse(null);
    }

    private boolean isAmountLimit(LimitType limitType) {
        return limitType.name().endsWith("_AMOUNT") || limitType == LimitType.OVERDRAFT_LIMIT
                || limitType == LimitType.CREDIT_CARD_LIMIT || limitType == LimitType.PER_TRANSACTION_AMOUNT;
    }

    private LimitValidationResponse buildBlockedResponse(UUID customerId, UUID accountId, UUID cardId,
                                                         LimitType limitType, TransactionBlock block,
                                                         List<TransactionBlock> allBlocks) {
        return LimitValidationResponse.builder()
                .allowed(false)
                .customerId(customerId)
                .accountId(accountId)
                .cardId(cardId)
                .limitType(limitType)
                .blockTriggered(true)
                .blockMessage("Transaction blocked: " + block.getReason())
                .activeBlocks(allBlocks)
                .build();
    }
}