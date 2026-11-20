package com.ibank.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Holder name is required")
    @Size(min = 2, max = 255, message = "Holder name must be between 2 and 255 characters")
    private String holderName;

    @NotBlank(message = "CPF is required")
    @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$", message = "CPF must be in format 123.456.789-00")
    private String cpf;

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[^@]+@[^@]+\\.[^@]+$", message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\+55\\d{2}\\d{9}$", message = "Phone must be in format +5511999999999")
    private String phone;

    public enum AccountType {
        CURRENT, SAVINGS, SALARY, PAYMENT
    }
}