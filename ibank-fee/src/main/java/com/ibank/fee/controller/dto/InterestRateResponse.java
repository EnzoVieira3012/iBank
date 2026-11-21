package com.ibank.fee.controller.dto;

import com.ibank.fee.model.InterestIndex;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InterestRateResponse {

    private UUID id;
    private InterestIndex indexType;
    private String productCode;
    private LocalDate rateDate;
    private BigDecimal rateValue;
    private BigDecimal spread;
    private BigDecimal effectiveRate;
    private String source;
    private Boolean active;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}