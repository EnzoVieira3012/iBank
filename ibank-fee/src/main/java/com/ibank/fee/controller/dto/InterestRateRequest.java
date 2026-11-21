package com.ibank.fee.controller.dto;

import com.ibank.fee.model.InterestIndex;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InterestRateRequest {

    @NotNull(message = "Index type is required")
    private InterestIndex indexType;

    @Size(max = 50, message = "Product code must not exceed 50 characters")
    private String productCode;

    @NotNull(message = "Rate date is required")
    private LocalDate rateDate;

    @NotNull(message = "Rate value is required")
    @DecimalMin(value = "0", message = "Rate value must be non-negative")
    @Digits(integer = 5, fraction = 8, message = "Rate value must have max 5 integer and 8 decimal digits")
    private BigDecimal rateValue;

    @Digits(integer = 5, fraction = 6, message = "Spread must have max 5 integer and 6 decimal digits")
    private BigDecimal spread;

    @Size(max = 50, message = "Source must not exceed 50 characters")
    private String source;

    private String metadata;
}