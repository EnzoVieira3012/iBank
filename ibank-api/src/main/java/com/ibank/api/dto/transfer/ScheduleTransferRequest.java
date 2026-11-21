package com.ibank.api.dto.transfer;

import com.ibank.core.util.Money;
import com.ibank.transfer.model.TransferType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleTransferRequest {

    @NotNull(message = "Payer account ID is required")
    private UUID payerAccountId;

    private UUID payeeAccountId;

    @NotBlank(message = "Payee name is required")
    @Size(max = 255, message = "Payee name max 255 characters")
    private String payeeName;

    @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$|^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$", message = "Invalid CPF/CNPJ format")
    private String payeeCpf;

    @Pattern(regexp = "^\\d{4}$", message = "Branch must be 4 digits")
    private String payeeBranch;

    @Size(max = 20, message = "Account number max 20 characters")
    private String payeeAccountNumber;

    @Pattern(regexp = "^\\d{8}$", message = "Bank ISPB must be 8 digits")
    private String payeeBankIspb;

    @Size(max = 100, message = "Bank name max 100 characters")
    private String payeeBankName;

    @NotNull(message = "Transfer type is required")
    private TransferType transferType;

    @NotNull(message = "Amount is required")
    private Money amount;

    @Size(max = 140, message = "Description max 140 characters")
    private String description;

    private Money feeAmount;

    @NotNull(message = "Scheduled date/time is required")
    @Future(message = "Scheduled time must be in the future")
    private OffsetDateTime scheduledAt;
}