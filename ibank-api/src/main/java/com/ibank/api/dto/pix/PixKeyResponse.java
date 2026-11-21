package com.ibank.api.dto.pix;

import com.ibank.pix.model.PixKeyStatus;
import com.ibank.pix.model.PixKeyType;
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
public class PixKeyResponse {

    private UUID id;
    private String keyValue;
    private PixKeyType keyType;
    private PixKeyStatus status;
    private UUID accountId;
    private String holderName;
    private String holderCpf;
    private String holderAccountType;
    private String holderBranch;
    private String holderAccountNumber;
    private String dictRegistrationId;
    private OffsetDateTime dictRegisteredAt;
    private OffsetDateTime blockedAt;
    private String blockedReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}