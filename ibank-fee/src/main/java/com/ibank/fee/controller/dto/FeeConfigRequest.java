package com.ibank.fee.controller.dto;

import com.ibank.fee.model.FeeCalculationType;
import com.ibank.fee.model.FeePeriod;
import com.ibank.fee.model.FeeType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FeeConfigRequest {

    @NotNull(message = "Fee type is required")
    private FeeType feeType;

    @Size(max = 50, message = "Product code must not exceed 50 characters")
    private String productCode;

    @Size(max = 20, message = "Channel must not exceed 20 characters")
    private String channel;

    @Size(max = 30, message = "Customer segment must not exceed 30 characters")
    private String customerSegment;

    @NotNull(message = "Period is required")
    private FeePeriod period;

    @NotNull(message = "Calculation type is required")
    private FeeCalculationType calculationType;

    @DecimalMin(value = "0", message = "Fixed amount must be non-negative")
    private BigDecimal fixedAmount;

    @DecimalMin(value = "0", message = "Percentage rate must be non-negative")
    @Digits(integer = 5, fraction = 6, message = "Percentage rate must have max 5 integer and 6 decimal digits")
    private BigDecimal percentageRate;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private String tierConfig;

    @Size(max = 500, message = "Formula must not exceed 500 characters")
    private String formula;

    private Boolean taxIncluded;

    @Digits(integer = 5, fraction = 6, message = "Tax rate must have max 5 integer and 6 decimal digits")
    private BigDecimal taxRate;

    @NotNull(message = "Valid from date is required")
    private LocalDate validFrom;

    private LocalDate validTo;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Integer priority;
}