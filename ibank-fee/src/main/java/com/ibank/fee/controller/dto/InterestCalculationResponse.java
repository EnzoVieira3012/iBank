package com.ibank.fee.controller.dto;

import com.ibank.fee.model.InterestIndex;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class InterestCalculationResponse {

    private InterestIndex indexType;
    private BigDecimal baseRate;
    private BigDecimal spread;
    private BigDecimal effectiveRate;
    private BigDecimal principal;
    private Integer days;
    private BigDecimal interestAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private LocalDate calculationDate;
    private String calculationMethod;
}