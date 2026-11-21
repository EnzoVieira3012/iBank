package com.ibank.limit.controller.dto;

import com.ibank.limit.model.BlockScope;
import com.ibank.limit.model.BlockType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TransactionBlockRequest {

    private String customerId;

    private String accountId;

    private String cardId;

    @NotNull(message = "Block type is required")
    private BlockType blockType;

    @NotNull(message = "Scope is required")
    private BlockScope scope;

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    @NotNull(message = "Start date/time is required")
    private LocalDateTime startsAt;

    private LocalDateTime expiresAt;

    @NotNull(message = "Created by is required")
    private String createdBy;

    private String conditions;

    @Size(max = 200, message = "Affected channels must not exceed 200 characters")
    private String affectedChannels;

    @Size(max = 200, message = "Affected transaction types must not exceed 200 characters")
    private String affectedTransactionTypes;

    private String metadata;
}