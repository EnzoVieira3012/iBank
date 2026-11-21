package com.ibank.antifraud.controller.dto;

import com.ibank.antifraud.model.FraudRuleAction;
import com.ibank.antifraud.model.FraudRuleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FraudRuleResponse {

    private UUID id;
    private String name;
    private String description;
    private FraudRuleType ruleType;
    private FraudRuleAction action;
    private Integer priority;
    private String conditions;
    private Integer scoreImpact;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean active;
    private String channel;
    private String productCode;
    private String customerSegment;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}