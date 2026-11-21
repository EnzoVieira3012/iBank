package com.ibank.fee.controller.dto;

import com.ibank.fee.model.FeeCalculationType;
import com.ibank.fee.model.FeePeriod;
import com.ibank.fee.model.FeeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FeeConfigResponse {

    private UUID id;
    private FeeType feeType;
    private String productCode;
    private String channel;
    private String customerSegment;
    private FeePeriod period;
    private FeeCalculationType calculationType;
    private BigDecimal fixedAmount;
    private BigDecimal percentageRate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String tierConfig;
    private String formula;
    private Boolean taxIncluded;
    private BigDecimal taxRate;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean active;
    private String description;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}