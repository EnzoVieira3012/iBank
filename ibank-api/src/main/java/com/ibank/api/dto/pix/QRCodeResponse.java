package com.ibank.api.dto.pix;

import com.ibank.core.util.Money;
import com.ibank.pix.model.QRCodeType;
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
public class QRCodeResponse {

    private UUID id;
    private String emv;
    private String payload;
    private QRCodeType qrCodeType;
    private Money amount;
    private String description;
    private UUID accountId;
    private UUID pixKeyId;
    private UUID transactionId;
    private OffsetDateTime expiresAt;
    private OffsetDateTime usedAt;
    private Integer usageCount;
    private Integer maxUsage;
    private String imageBase64;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}