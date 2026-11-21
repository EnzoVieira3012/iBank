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
public class CreateInternalTransferRequest {

    @NotNull(message = "Payer account ID is required")
    private UUID payerAccountId;

    @NotNull(message = "Payee account ID is required")
    private UUID payeeAccountId;

    @NotNull(message = "Amount is required")
    private Money amount;

    @Size(max = 140, message = "Description max 140 characters")
    private String description;

    private Money feeAmount;
}