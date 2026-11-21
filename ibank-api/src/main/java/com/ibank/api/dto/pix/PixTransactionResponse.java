package com.ibank.api.dto.pix;

import com.ibank.core.util.Money;
import com.ibank.pix.model.PixTransactionStatus;
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
public class PixTransactionResponse {

    private UUID id;
    private String endToEndId;
    private PixTransactionStatus status;
    private Money amount;
    private String description;
    // Payer
    private UUID payerAccountId;
    private String payerName;
    private String payerCpf;
    private UUID payerKeyId;
    private String payerKeyValue;
    private String payerKeyType;
    private String payerBranch;
    private String payerAccountNumber;
    // Payee
    private UUID payeeAccountId;
    private String payeeName;
    private String payeeCpf;
    private UUID payeeKeyId;
    private String payeeKeyValue;
    private String payeeKeyType;
    private String payeeBranch;
    private String payeeAccountNumber;
    // Refund
    private UUID refundId;
    private String refundEndToEndId;
    private Money refundedAmount;
    private String refundReason;
    private OffsetDateTime refundedAt;
    // Processing
    private OffsetDateTime processedAt;
    private OffsetDateTime failedAt;
    private String failureReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}