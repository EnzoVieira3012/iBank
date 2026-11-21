package com.ibank.api.dto.pix;

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
public class SendPixRequest {

    @NotNull(message = "Payer account ID is required")
    private UUID payerAccountId;

    @NotBlank(message = "Payer key value is required")
    private String payerKeyValue;

    private UUID payerKeyId;

    @NotBlank(message = "Payer key type is required")
    private String payerKeyType;

    @NotBlank(message = "Payee key value is required")
    private String payeeKeyValue;

    @NotNull(message = "Amount is required")
    private Money amount;

    @Size(max = 140, message = "Description max 140 characters")
    private String description;
}