package com.ibank.limit.controller.dto;

import com.ibank.limit.model.LimitScope;
import com.ibank.limit.model.LimitType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CustomerLimitResponse {

    private UUID id;
    private UUID customerId;
    private UUID accountId;
    private UUID cardId;
    private LimitType limitType;
    private LimitScope scope;
    private BigDecimal limitValue;
    private BigDecimal currentUsage;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean active;
    private BigDecimal warningThreshold;
    private Boolean blockOnExceed;
    private String conditions;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}