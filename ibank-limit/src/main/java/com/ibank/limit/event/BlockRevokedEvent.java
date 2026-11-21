package com.ibank.limit.event;

import com.ibank.limit.model.BlockType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class BlockRevokedEvent extends ApplicationEvent {
    private final UUID blockId;
    private final UUID customerId;
    private final UUID accountId;
    private final UUID cardId;
    private final BlockType blockType;
    private final String reason;
    private final UUID revokedBy;

    public BlockRevokedEvent(Object source, UUID blockId, UUID customerId, UUID accountId, UUID cardId,
                             BlockType blockType, String reason, UUID revokedBy) {
        super(source);
        this.blockId = blockId;
        this.customerId = customerId;
        this.accountId = accountId;
        this.cardId = cardId;
        this.blockType = blockType;
        this.reason = reason;
        this.revokedBy = revokedBy;
    }
}