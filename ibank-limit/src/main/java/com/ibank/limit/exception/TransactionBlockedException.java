package com.ibank.limit.exception;

import com.ibank.core.exception.BusinessException;
import com.ibank.limit.model.BlockType;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TransactionBlockedException extends BusinessException {
    private final BlockType blockType;
    private final String blockScope;
    private final String reason;
    private final UUID blockId;

    public TransactionBlockedException(String message, BlockType blockType, String blockScope,
                                       String reason, UUID blockId) {
        super("TRANSACTION_BLOCKED", message);
        this.blockType = blockType;
        this.blockScope = blockScope;
        this.reason = reason;
        this.blockId = blockId;
    }
}