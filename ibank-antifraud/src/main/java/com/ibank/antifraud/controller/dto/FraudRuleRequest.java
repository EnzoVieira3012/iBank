package com.ibank.antifraud.controller.dto;

import com.ibank.antifraud.model.FraudRuleAction;
import com.ibank.antifraud.model.FraudRuleType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FraudRuleRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Rule type is required")
    private FraudRuleType ruleType;

    @NotNull(message = "Action is required")
    private FraudRuleAction action;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 1000, message = "Priority must not exceed 1000")
    private Integer priority;

    @NotBlank(message = "Conditions JSON is required")
    private String conditions;

    @Min(value = -100, message = "Score impact must be >= -100")
    @Max(value = 100, message = "Score impact must be <= 100")
    private Integer scoreImpact;

    @NotNull(message = "Valid from date is required")
    private LocalDate validFrom;

    private LocalDate validTo;

    @Size(max = 20, message = "Channel must not exceed 20 characters")
    private String channel;

    @Size(max = 50, message = "Product code must not exceed 50 characters")
    private String productCode;

    @Size(max = 30, message = "Customer segment must not exceed 30 characters")
    private String customerSegment;

    private String createdBy;
}