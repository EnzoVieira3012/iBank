package com.ibank.api.dto.transfer;

import com.ibank.core.util.Money;
import com.ibank.transfer.model.TransferStatus;
import com.ibank.transfer.model.TransferType;
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
public class TransferResponse {

    private UUID id;
    private String idempotencyKey;
    private String endToEndId;
    private TransferType transferType;
    private TransferStatus status;
    private Money amount;
    private String description;
    // Payer
    private UUID payerAccountId;
    private String payerName;
    private String payerCpf;
    private String payerBranch;
    private String payerAccountNumber;
    private String payerBankIspb;
    // Payee
    private UUID payeeAccountId;
    private String payeeName;
    private String payeeCpf;
    private String payeeBranch;
    private String payeeAccountNumber;
    private String payeeBankIspb;
    private String payeeBankName;
    // Scheduling
    private OffsetDateTime scheduledAt;
    private OffsetDateTime executedAt;
    private OffsetDateTime failedAt;
    private String failureReason;
    private OffsetDateTime cancelledAt;
    private String cancelledReason;
    // Fee
    private Money feeAmount;
    private OffsetDateTime feeChargedAt;
    // External
    private String externalReference;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}