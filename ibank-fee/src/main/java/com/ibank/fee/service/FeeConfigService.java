package com.ibank.fee.service;

import com.ibank.fee.controller.dto.FeeConfigRequest;
import com.ibank.fee.exception.FeeConfigNotFoundException;
import com.ibank.fee.mapper.FeeConfigMapper;
import com.ibank.fee.model.FeeConfig;
import com.ibank.fee.model.FeeType;
import com.ibank.fee.repository.FeeConfigRepository;
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
public class FeeConfigService {

    private final FeeConfigRepository feeConfigRepository;
    private final FeeConfigMapper feeConfigMapper;

    @Transactional
    public FeeConfig create(FeeConfigRequest request) {
        FeeConfig config = feeConfigMapper.toEntity(request);
        config.setActive(true);
        FeeConfig saved = feeConfigRepository.save(config);
        log.info("Fee config created: {} for type {}", saved.getId(), saved.getFeeType());
        return saved;
    }

    @Transactional(readOnly = true)
    public FeeConfig getById(UUID id) {
        return feeConfigRepository.findById(id)
                .orElseThrow(() -> new FeeConfigNotFoundException("Fee config not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<FeeConfig> findAll() {
        return feeConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FeeConfig> findByType(FeeType feeType) {
        return feeConfigRepository.findByFeeType(feeType);
    }

    @Transactional(readOnly = true)
    public List<FeeConfig> findActiveByType(FeeType feeType) {
        return feeConfigRepository.findByFeeTypeAndActiveTrue(feeType);
    }

    @Transactional(readOnly = true)
    public List<FeeConfig> findByProductCode(String productCode) {
        return feeConfigRepository.findByProductCode(productCode);
    }

    @Transactional(readOnly = true)
    public List<FeeConfig> findApplicable(FeeType feeType, LocalDate date, String productCode,
                                          String channel, String customerSegment) {
        return feeConfigRepository.findApplicableConfigs(feeType, date, productCode, channel, customerSegment);
    }

    @Transactional
    public FeeConfig update(UUID id, FeeConfigRequest request) {
        FeeConfig config = getById(id);
        config.setFeeType(request.getFeeType());
        config.setProductCode(request.getProductCode());
        config.setChannel(request.getChannel());
        config.setCustomerSegment(request.getCustomerSegment());
        config.setPeriod(request.getPeriod());
        config.setCalculationType(request.getCalculationType());
        config.setFixedAmount(request.getFixedAmount());
        config.setPercentageRate(request.getPercentageRate());
        config.setMinAmount(request.getMinAmount());
        config.setMaxAmount(request.getMaxAmount());
        config.setTierConfig(request.getTierConfig());
        config.setFormula(request.getFormula());
        config.setTaxIncluded(request.getTaxIncluded());
        config.setTaxRate(request.getTaxRate());
        config.setValidFrom(request.getValidFrom());
        config.setValidTo(request.getValidTo());
        config.setDescription(request.getDescription());
        config.setPriority(request.getPriority());
        FeeConfig saved = feeConfigRepository.save(config);
        log.info("Fee config updated: {}", id);
        return saved;
    }

    @Transactional
    public void activate(UUID id) {
        FeeConfig config = getById(id);
        config.setActive(true);
        feeConfigRepository.save(config);
    }

    @Transactional
    public void deactivate(UUID id) {
        FeeConfig config = getById(id);
        config.setActive(false);
        feeConfigRepository.save(config);
    }

    @Transactional
    public void delete(UUID id) {
        FeeConfig config = getById(id);
        feeConfigRepository.delete(config);
        log.info("Fee config deleted: {}", id);
    }
}