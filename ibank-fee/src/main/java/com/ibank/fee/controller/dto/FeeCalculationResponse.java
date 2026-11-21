package com.ibank.fee.controller.dto;

import com.ibank.fee.model.FeeCalculationType;
import com.ibank.fee.model.FeeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FeeCalculationResponse {

    private UUID feeConfigId;
    private FeeType feeType;
    private FeeCalculationType calculationType;
    private BigDecimal baseAmount;
    private BigDecimal fixedAmount;
    private BigDecimal percentageRate;
    private BigDecimal calculatedFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String appliedFormula;
    private List<TierDetail> appliedTiers;

    @Data
    @Builder
    public static class TierDetail {
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private BigDecimal rate;
        private BigDecimal fee;
    }
}