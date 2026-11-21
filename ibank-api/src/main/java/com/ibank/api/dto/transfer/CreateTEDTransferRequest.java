package com.ibank.api.dto.transfer;

import com.ibank.core.util.Money;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTEDTransferRequest {

    @NotNull(message = "Payer account ID is required")
    private UUID payerAccountId;

    @NotBlank(message = "Payee name is required")
    @Size(max = 255, message = "Payee name max 255 characters")
    private String payeeName;

    @NotBlank(message = "Payee CPF is required")
    @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$|^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$", message = "Invalid CPF/CNPJ format")
    private String payeeCpf;

    @NotBlank(message = "Payee branch is required")
    @Pattern(regexp = "^\\d{4}$", message = "Branch must be 4 digits")
    private String payeeBranch;

    @NotBlank(message = "Payee account number is required")
    @Size(max = 20, message = "Account number max 20 characters")
    private String payeeAccountNumber;

    @NotBlank(message = "Payee bank ISPB is required")
    @Pattern(regexp = "^\\d{8}$", message = "Bank ISPB must be 8 digits")
    private String payeeBankIspb;

    @NotBlank(message = "Payee bank name is required")
    @Size(max = 100, message = "Bank name max 100 characters")
    private String payeeBankName;

    @NotNull(message = "Amount is required")
    private Money amount;

    @Size(max = 140, message = "Description max 140 characters")
    private String description;

    private Money feeAmount;
}