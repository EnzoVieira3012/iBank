package com.ibank.limit.event;

import com.ibank.limit.model.BlockScope;
import com.ibank.limit.model.BlockStatus;
import com.ibank.limit.model.BlockType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class BlockCreatedEvent extends ApplicationEvent {
    private final UUID blockId;
    private final UUID customerId;
    private final UUID accountId;
    private final UUID cardId;
    private final BlockType blockType;
    private final BlockScope scope;
    private final BlockStatus status;
    private final String reason;
    private final LocalDateTime startsAt;
    private final LocalDateTime expiresAt;
    private final UUID createdBy;

    public BlockCreatedEvent(Object source, UUID blockId, UUID customerId, UUID accountId, UUID cardId,
                             BlockType blockType, BlockScope scope, BlockStatus status, String reason,
                             LocalDateTime startsAt, LocalDateTime expiresAt, UUID createdBy) {
        super(source);
        this.blockId = blockId;
        this.customerId = customerId;
        this.accountId = accountId;
        this.cardId = cardId;
        this.blockType = blockType;
        this.scope = scope;
        this.status = status;
        this.reason = reason;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
    }
}