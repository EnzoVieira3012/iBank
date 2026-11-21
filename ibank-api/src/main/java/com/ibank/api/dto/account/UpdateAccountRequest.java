package com.ibank.api.dto.account;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {

    @Size(min = 2, max = 255, message = "Holder name must be between 2 and 255 characters")
    private String holderName;

    @Email(message = "Invalid email format")
    private String holderEmail;

    @Pattern(regexp = "^\\+55\\d{2}\\d{9}$", message = "Phone must be in format +5511999999999")
    private String holderPhone;

    private Boolean pixEnabled;
    private Boolean debitCardEnabled;
    private Boolean creditCardEnabled;

    @DecimalMin(value = "0.01", message = "Daily withdrawal limit must be positive")
    private BigDecimal dailyWithdrawalLimit;

    @DecimalMin(value = "0.01", message = "Daily transfer limit must be positive")
    private BigDecimal dailyTransferLimit;

    @DecimalMin(value = "0.01", message = "Monthly transfer limit must be positive")
    private BigDecimal monthlyTransferLimit;

    @DecimalMin(value = "0.01", message = "PIX limit must be positive")
    private BigDecimal pixLimit;

    @DecimalMin(value = "0.01", message = "Overdraft limit must be positive")
    private BigDecimal overdraftLimit;
}