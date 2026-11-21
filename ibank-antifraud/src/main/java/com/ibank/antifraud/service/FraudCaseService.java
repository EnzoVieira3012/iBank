package com.ibank.antifraud.service;

import com.ibank.antifraud.controller.dto.FraudCaseRequest;
import com.ibank.antifraud.event.FraudCaseCreatedEvent;
import com.ibank.antifraud.exception.FraudCaseNotFoundException;
import com.ibank.antifraud.mapper.FraudCaseMapper;
import com.ibank.antifraud.model.CaseStatus;
import com.ibank.antifraud.model.FraudCase;
import com.ibank.antifraud.repository.FraudCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudCaseService {

    private final FraudCaseRepository caseRepository;
    private final FraudCaseMapper caseMapper;
    private final FraudAlertService fraudAlertService;

    @Transactional
    public FraudCase create(FraudCaseRequest request) {
        FraudCase fraudCase = caseMapper.toEntity(request);
        fraudCase.setCustomerId(UUID.fromString(request.getCustomerId()));
        if (request.getAccountId() != null) {
            fraudCase.setAccountId(UUID.fromString(request.getAccountId()));
        }
        if (request.getCardId() != null) {
            fraudCase.setCardId(UUID.fromString(request.getCardId()));
        }
        fraudCase.setCaseNumber(generateCaseNumber());
        fraudCase.setAlertIds(request.getAlertIds());

        FraudCase saved = caseRepository.save(fraudCase);

        // Link alerts to case
        if (request.getAlertIds() != null) {
            linkAlertsToCase(saved.getId(), request.getAlertIds());
        }

        log.info("Fraud case created: {}", saved.getCaseNumber());
        return saved;
    }

    private String generateCaseNumber() {
        return "FRAUD-" + System.currentTimeMillis();
    }

    private void linkAlertsToCase(UUID caseId, String alertIdsJson) {
        try {
            List<UUID> alertIds = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(alertIdsJson, List.class);
            for (UUID alertId : alertIds) {
                // Update alert with case ID would be done here
                log.debug("Linking alert {} to case {}", alertId, caseId);
            }
        } catch (Exception e) {
            log.warn("Failed to parse alert IDs: {}", alertIdsJson);
        }
    }

    @Transactional(readOnly = true)
    public FraudCase getById(UUID id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new FraudCaseNotFoundException("Fraud case not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<FraudCase> findByCaseNumber(String caseNumber) {
        return caseRepository.findByCaseNumber(caseNumber);
    }

    @Transactional(readOnly = true)
    public List<FraudCase> findAll() {
        return caseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FraudCase> findByCustomerId(UUID customerId) {
        return caseRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<FraudCase> findByStatus(CaseStatus status) {
        return caseRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<FraudCase> findByAssignedTo(UUID assignedTo) {
        return caseRepository.findByAssignedTo(assignedTo);
    }

    @Transactional(readOnly = true)
    public List<FraudCase> findOpenCases() {
        return caseRepository.findOpenCases(List.of(
                CaseStatus.OPEN, CaseStatus.INVESTIGATING, CaseStatus.PENDING_INFO, CaseStatus.ESCALATED
        ));
    }

    @Transactional(readOnly = true)
    public List<FraudCase> findAssignedOpenCases(UUID assignedTo) {
        return caseRepository.findAssignedOpenCases(assignedTo, List.of(
                CaseStatus.OPEN, CaseStatus.INVESTIGATING, CaseStatus.PENDING_INFO, CaseStatus.ESCALATED
        ));
    }

    @Transactional
    public FraudCase assignCase(UUID caseId, UUID assignedTo) {
        FraudCase fraudCase = getById(caseId);
        fraudCase.setAssignedTo(assignedTo);
        fraudCase.setAssignedAt(LocalDateTime.now());
        fraudCase.setStatus(CaseStatus.INVESTIGATING);
        FraudCase saved = caseRepository.save(fraudCase);
        log.info("Case {} assigned to {}", caseId, assignedTo);
        return saved;
    }

    @Transactional
    public FraudCase updateInvestigation(UUID caseId, UUID investigatorId, String notes) {
        FraudCase fraudCase = getById(caseId);
        fraudCase.setInvestigatorNotes(notes);
        FraudCase saved = caseRepository.save(fraudCase);
        log.info("Case {} investigation updated by {}", caseId, investigatorId);
        return saved;
    }

    @Transactional
    public FraudCase resolveCase(UUID caseId, UUID resolvedBy, CaseStatus resolution, String resolutionNotes) {
        FraudCase fraudCase = getById(caseId);
        fraudCase.setStatus(resolution);
        fraudCase.setResolvedBy(resolvedBy);
        fraudCase.setResolvedAt(LocalDateTime.now());
        fraudCase.setResolution(resolutionNotes);
        FraudCase saved = caseRepository.save(fraudCase);
        log.info("Case {} resolved as {} by {}", caseId, resolution, resolvedBy);
        return saved;
    }

    @Transactional
    public FraudCase fileSar(UUID caseId, UUID filedBy, String sarReference) {
        FraudCase fraudCase = getById(caseId);
        fraudCase.setSarFiled(true);
        fraudCase.setSarFiledAt(LocalDateTime.now());
        fraudCase.setSarReference(sarReference);
        fraudCase.setStatus(CaseStatus.SAR_FILED);
        FraudCase saved = caseRepository.save(fraudCase);
        log.info("SAR filed for case {}: {}", caseId, sarReference);
        return saved;
    }

    @Transactional
    public FraudCase escalateCase(UUID caseId, UUID escalatedTo) {
        FraudCase fraudCase = getById(caseId);
        fraudCase.setStatus(CaseStatus.ESCALATED);
        fraudCase.setEscalatedAt(LocalDateTime.now());
        fraudCase.setEscalatedTo(escalatedTo);
        FraudCase saved = caseRepository.save(fraudCase);
        log.info("Case {} escalated to {}", caseId, escalatedTo);
        return saved;
    }

    @Transactional(readOnly = true)
    public long countSarFiledInPeriod(LocalDate start, LocalDate end) {
        return caseRepository.countSarFiledInPeriod(start, end);
    }
}