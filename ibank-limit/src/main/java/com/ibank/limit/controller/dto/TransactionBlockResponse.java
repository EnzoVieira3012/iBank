package com.ibank.limit.controller.dto;

import com.ibank.limit.model.BlockScope;
import com.ibank.limit.model.BlockStatus;
import com.ibank.limit.model.BlockType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransactionBlockResponse {

    private UUID id;
    private UUID customerId;
    private UUID accountId;
    private UUID cardId;
    private BlockType blockType;
    private BlockScope scope;
    private BlockStatus status;
    private String reason;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private UUID createdBy;
    private UUID approvedBy;
    private LocalDateTime approvedAt;
    private UUID revokedBy;
    private LocalDateTime revokedAt;
    private String revokeReason;
    private String conditions;
    private String affectedChannels;
    private String affectedTransactionTypes;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}