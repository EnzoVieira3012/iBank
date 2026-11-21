package com.ibank.antifraud.service;

import com.ibank.antifraud.controller.dto.FraudAlertRequest;
import com.ibank.antifraud.exception.FraudAlertNotFoundException;
import com.ibank.antifraud.mapper.FraudAlertMapper;
import com.ibank.antifraud.model.AlertSeverity;
import com.ibank.antifraud.model.AlertStatus;
import com.ibank.antifraud.model.FraudAlert;
import com.ibank.antifraud.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAlertService {

    private final FraudAlertRepository alertRepository;
    private final FraudAlertMapper alertMapper;

    @Transactional(readOnly = true)
    public FraudAlert getById(UUID id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new FraudAlertNotFoundException("Fraud alert not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> findAll() {
        return alertRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> findByCustomerId(UUID customerId) {
        return alertRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> findByAccountId(UUID accountId) {
        return alertRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> findByCardId(UUID cardId) {
        return alertRepository.findByCardId(cardId);
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> findByTransactionId(UUID transactionId) {
        return alertRepository.findByTransactionId(transactionId);
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> findByStatus(AlertStatus status) {
        return alertRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> findBySeverity(AlertSeverity severity) {
        return alertRepository.findBySeverity(severity);
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> findByAssignedTo(UUID assignedTo) {
        return alertRepository.findByAssignedTo(assignedTo);
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> findByRuleId(UUID ruleId) {
        return alertRepository.findByRuleId(ruleId);
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> findOpenAlerts() {
        return alertRepository.findOpenAlerts(List.of(AlertStatus.OPEN, AlertStatus.IN_REVIEW, AlertStatus.ESCALATED));
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> findAssignedOpenAlerts(UUID assignedTo) {
        return alertRepository.findAssignedOpenAlerts(assignedTo, List.of(AlertStatus.OPEN, AlertStatus.IN_REVIEW, AlertStatus.ESCALATED));
    }

    @Transactional
    public FraudAlert assignAlert(UUID alertId, UUID assignedTo) {
        FraudAlert alert = getById(alertId);
        alert.setStatus(AlertStatus.IN_REVIEW);
        alert.setAssignedTo(assignedTo);
        alert.setAssignedAt(LocalDateTime.now());
        FraudAlert saved = alertRepository.save(alert);
        log.info("Alert {} assigned to {}", alertId, assignedTo);
        return saved;
    }

    @Transactional
    public FraudAlert reviewAlert(UUID alertId, UUID reviewedBy, AlertStatus newStatus, String resolution) {
        FraudAlert alert = getById(alertId);
        alert.setStatus(newStatus);
        alert.setReviewedBy(reviewedBy);
        alert.setReviewedAt(LocalDateTime.now());
        alert.setResolution(resolution);
        FraudAlert saved = alertRepository.save(alert);
        log.info("Alert {} reviewed by {} with status {}", alertId, reviewedBy, newStatus);
        return saved;
    }

    @Transactional
    public FraudAlert markFalsePositive(UUID alertId, UUID reviewedBy, String reason) {
        FraudAlert alert = getById(alertId);
        alert.setStatus(AlertStatus.FALSE_POSITIVE);
        alert.setReviewedBy(reviewedBy);
        alert.setReviewedAt(LocalDateTime.now());
        alert.setFalsePositiveReason(reason);
        FraudAlert saved = alertRepository.save(alert);
        log.info("Alert {} marked as false positive by {}", alertId, reviewedBy);
        return saved;
    }

    @Transactional
    public FraudAlert escalateAlert(UUID alertId, UUID escalatedTo) {
        FraudAlert alert = getById(alertId);
        alert.setStatus(AlertStatus.ESCALATED);
        alert.setEscalatedTo(escalatedTo);
        alert.setEscalatedAt(LocalDateTime.now());
        FraudAlert saved = alertRepository.save(alert);
        log.info("Alert {} escalated to {}", alertId, escalatedTo);
        return saved;
    }

    @Transactional
    public FraudAlert autoCloseAlerts(LocalDateTime olderThan) {
        List<FraudAlert> alerts = alertRepository.findByCreatedAtBefore(olderThan);
        int count = 0;
        for (FraudAlert alert : alerts) {
            if (alert.getStatus() == AlertStatus.OPEN || alert.getStatus() == AlertStatus.IN_REVIEW) {
                alert.setStatus(AlertStatus.AUTO_CLOSED);
                alert.setReviewedAt(LocalDateTime.now());
                alert.setResolution("Auto-closed due to age");
                alertRepository.save(alert);
                count++;
            }
        }
        log.info("Auto-closed {} fraud alerts", count);
        return null;
    }
}