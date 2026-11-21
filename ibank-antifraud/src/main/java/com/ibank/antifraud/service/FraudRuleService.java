package com.ibank.antifraud.service;

import com.ibank.antifraud.controller.dto.FraudRuleRequest;
import com.ibank.antifraud.exception.FraudRuleNotFoundException;
import com.ibank.antifraud.mapper.FraudRuleMapper;
import com.ibank.antifraud.model.FraudRule;
import com.ibank.antifraud.model.FraudRuleType;
import com.ibank.antifraud.repository.FraudRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudRuleService {

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudRuleMapper fraudRuleMapper;

    @Transactional
    public FraudRule create(FraudRuleRequest request) {
        FraudRule rule = fraudRuleMapper.toEntity(request);
        rule.setCreatedBy(UUID.fromString(request.getCreatedBy()));
        FraudRule saved = fraudRuleRepository.save(rule);
        log.info("Fraud rule created: {} ({})", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional(readOnly = true)
    public FraudRule getById(UUID id) {
        return fraudRuleRepository.findById(id)
                .orElseThrow(() -> new FraudRuleNotFoundException("Fraud rule not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<FraudRule> findAll() {
        return fraudRuleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FraudRule> findByType(FraudRuleType ruleType) {
        return fraudRuleRepository.findByRuleType(ruleType);
    }

    @Transactional(readOnly = true)
    public List<FraudRule> findActive() {
        return fraudRuleRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<FraudRule> findActiveOrderByPriority() {
        return fraudRuleRepository.findByActiveTrueOrderByPriorityAsc();
    }

    @Transactional(readOnly = true)
    public List<FraudRule> findApplicable(String channel, String productCode, String customerSegment) {
        return fraudRuleRepository.findApplicableRules(LocalDate.now(), channel, productCode, customerSegment);
    }

    @Transactional(readOnly = true)
    public Optional<FraudRule> findByName(String name) {
        return fraudRuleRepository.findByName(name);
    }

    @Transactional
    public FraudRule update(UUID id, FraudRuleRequest request) {
        FraudRule rule = getById(id);
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setRuleType(request.getRuleType());
        rule.setAction(request.getAction());
        rule.setPriority(request.getPriority());
        rule.setConditions(request.getConditions());
        rule.setScoreImpact(request.getScoreImpact());
        rule.setValidFrom(request.getValidFrom());
        rule.setValidTo(request.getValidTo());
        rule.setChannel(request.getChannel());
        rule.setProductCode(request.getProductCode());
        rule.setCustomerSegment(request.getCustomerSegment());
        FraudRule saved = fraudRuleRepository.save(rule);
        log.info("Fraud rule updated: {}", id);
        return saved;
    }

    @Transactional
    public void activate(UUID id) {
        FraudRule rule = getById(id);
        rule.setActive(true);
        fraudRuleRepository.save(rule);
    }

    @Transactional
    public void deactivate(UUID id) {
        FraudRule rule = getById(id);
        rule.setActive(false);
        fraudRuleRepository.save(rule);
    }

    @Transactional
    public void delete(UUID id) {
        FraudRule rule = getById(id);
        fraudRuleRepository.delete(rule);
        log.info("Fraud rule deleted: {}", id);
    }
}