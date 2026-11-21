package com.ibank.pix.event;

import com.ibank.core.util.Money;
import com.ibank.pix.model.QRCodeType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class QRCodeGeneratedEvent extends ApplicationEvent {

    private final UUID qrCodeId;
    private final String emv;
    private final QRCodeType qrCodeType;
    private final UUID accountId;
    private final UUID pixKeyId;
    private final Money amount;
    private final String description;

    public QRCodeGeneratedEvent(UUID qrCodeId, String emv, QRCodeType qrCodeType,
                                UUID accountId, UUID pixKeyId, Money amount, String description) {
        super(qrCodeId);
        this.qrCodeId = qrCodeId;
        this.emv = emv;
        this.qrCodeType = qrCodeType;
        this.accountId = accountId;
        this.pixKeyId = pixKeyId;
        this.amount = amount;
        this.description = description;
    }
}