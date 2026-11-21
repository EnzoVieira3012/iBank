package com.ibank.api.dto.account;

import com.ibank.core.model.AccountType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAccountRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Holder name is required")
    @Size(min = 2, max = 255, message = "Holder name must be between 2 and 255 characters")
    private String holderName;

    @NotBlank(message = "CPF is required")
    @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$", message = "CPF must be in format 123.456.789-00")
    private String holderCpf;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String holderEmail;

    @Pattern(regexp = "^\\+55\\d{2}\\d{9}$", message = "Phone must be in format +5511999999999")
    private String holderPhone;

    private LocalDate holderBirthDate;

    @Pattern(regexp = "^\\d{4}$", message = "Branch must be 4 digits")
    private String branch;

    private BigDecimal initialDeposit;
}