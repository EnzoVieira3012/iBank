package com.ibank.limit.controller.dto;

import com.ibank.limit.model.LimitScope;
import com.ibank.limit.model.LimitType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CustomerLimitRequest {

    @NotNull(message = "Customer ID is required")
    private String customerId;

    private String accountId;

    private String cardId;

    @NotNull(message = "Limit type is required")
    private LimitType limitType;

    @NotNull(message = "Scope is required")
    private LimitScope scope;

    @NotNull(message = "Limit value is required")
    @DecimalMin(value = "0.01", message = "Limit value must be greater than zero")
    private BigDecimal limitValue;

    @NotNull(message = "Valid from date is required")
    private LocalDate validFrom;

    private LocalDate validTo;

    @DecimalMin(value = "0", message = "Warning threshold must be non-negative")
    @Digits(integer = 3, fraction = 2, message = "Warning threshold must be a percentage (0-100)")
    private BigDecimal warningThreshold;

    private Boolean blockOnExceed;

    private String conditions;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}