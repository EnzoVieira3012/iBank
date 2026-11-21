package com.ibank.pix.event;

import com.ibank.pix.model.PixKeyType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class PixKeyRegisteredEvent extends ApplicationEvent {

    private final UUID keyId;
    private final String keyValue;
    private final PixKeyType keyType;
    private final UUID accountId;
    private final String holderCpf;
    private final String idempotencyKey;

    public PixKeyRegisteredEvent(UUID keyId, String keyValue, PixKeyType keyType,
                                 UUID accountId, String holderCpf, String idempotencyKey) {
        super(keyId);
        this.keyId = keyId;
        this.keyValue = keyValue;
        this.keyType = keyType;
        this.accountId = accountId;
        this.holderCpf = holderCpf;
        this.idempotencyKey = idempotencyKey;
    }
}