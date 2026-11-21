package com.ibank.limit.service;

import com.ibank.limit.controller.dto.CustomerLimitRequest;
import com.ibank.limit.controller.dto.CustomerLimitResponse;
import com.ibank.limit.exception.LimitNotFoundException;
import com.ibank.limit.mapper.CustomerLimitMapper;
import com.ibank.limit.model.CustomerLimit;
import com.ibank.limit.model.LimitScope;
import com.ibank.limit.model.LimitType;
import com.ibank.limit.repository.CustomerLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerLimitService {

    private final CustomerLimitRepository limitRepository;
    private final CustomerLimitMapper limitMapper;

    @Transactional
    public CustomerLimit create(CustomerLimitRequest request) {
        CustomerLimit limit = limitMapper.toEntity(request);
        limit.setCustomerId(UUID.fromString(request.getCustomerId()));
        if (request.getAccountId() != null) {
            limit.setAccountId(UUID.fromString(request.getAccountId()));
        }
        if (request.getCardId() != null) {
            limit.setCardId(UUID.fromString(request.getCardId()));
        }
        limit.setActive(true);
        limit.setCurrentUsage(new java.math.BigDecimal("0"));
        limit.setPeriodStart(java.time.LocalDateTime.now());
        limit.setPeriodEnd(calculatePeriodEnd(limit.getLimitType(), java.time.LocalDateTime.now()));

        CustomerLimit saved = limitRepository.save(limit);
        log.info("Customer limit created: {} for customer {}", saved.getId(), saved.getCustomerId());
        return saved;
    }

    @Transactional(readOnly = true)
    public CustomerLimit getById(UUID id) {
        return limitRepository.findById(id)
                .orElseThrow(() -> new LimitNotFoundException("Limit not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<CustomerLimit> findAll() {
        return limitRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CustomerLimit> findByCustomerId(UUID customerId) {
        return limitRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<CustomerLimit> findActiveByCustomerId(UUID customerId) {
        return limitRepository.findByCustomerIdAndActiveTrue(customerId);
    }

    @Transactional(readOnly = true)
    public List<CustomerLimit> findByAccountId(UUID accountId) {
        return limitRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<CustomerLimit> findByCardId(UUID cardId) {
        return limitRepository.findByCardId(cardId);
    }

    @Transactional(readOnly = true)
    public List<CustomerLimit> findByLimitType(LimitType limitType) {
        return limitRepository.findByLimitType(limitType);
    }

    @Transactional(readOnly = true)
    public List<CustomerLimit> findByScope(LimitScope scope) {
        return limitRepository.findByScope(scope);
    }

    @Transactional(readOnly = true)
    public List<CustomerLimit> findApplicableLimits(UUID customerId, UUID accountId, UUID cardId) {
        return limitRepository.findApplicableLimits(customerId, LocalDate.now(), accountId, cardId);
    }

    @Transactional
    public CustomerLimit update(UUID id, CustomerLimitRequest request) {
        CustomerLimit limit = getById(id);
        limit.setLimitType(request.getLimitType());
        limit.setScope(request.getScope());
        limit.setLimitValue(request.getLimitValue());
        limit.setValidFrom(request.getValidFrom());
        limit.setValidTo(request.getValidTo());
        limit.setWarningThreshold(request.getWarningThreshold());
        limit.setBlockOnExceed(request.getBlockOnExceed());
        limit.setConditions(request.getConditions());
        limit.setDescription(request.getDescription());

        if (request.getAccountId() != null) {
            limit.setAccountId(UUID.fromString(request.getAccountId()));
        } else {
            limit.setAccountId(null);
        }
        if (request.getCardId() != null) {
            limit.setCardId(UUID.fromString(request.getCardId()));
        } else {
            limit.setCardId(null);
        }

        CustomerLimit saved = limitRepository.save(limit);
        log.info("Customer limit updated: {}", id);
        return saved;
    }

    @Transactional
    public void activate(UUID id) {
        CustomerLimit limit = getById(id);
        limit.setActive(true);
        limitRepository.save(limit);
    }

    @Transactional
    public void deactivate(UUID id) {
        CustomerLimit limit = getById(id);
        limit.setActive(false);
        limitRepository.save(limit);
    }

    @Transactional
    public void delete(UUID id) {
        CustomerLimit limit = getById(id);
        limitRepository.delete(limit);
        log.info("Customer limit deleted: {}", id);
    }

    @Transactional
    public void resetUsage(UUID customerId, UUID accountId, UUID cardId, LimitType limitType) {
        // Usage reset is handled by the period calculation in validation service
        log.info("Usage reset requested for customer {} type {}", customerId, limitType);
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
}