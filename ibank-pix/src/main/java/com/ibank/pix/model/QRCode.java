package com.ibank.pix.model;

import com.ibank.core.model.BaseEntity;
import com.ibank.core.util.Money;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pix_qr_codes", indexes = {
    @Index(name = "idx_qrcode_account", columnList = "account_id"),
    @Index(name = "idx_qrcode_key", columnList = "pix_key_id"),
    @Index(name = "idx_qrcode_tx_id", columnList = "transaction_id"),
    @Index(name = "idx_qrcode_emv", columnList = "emv", unique = true),
    @Index(name = "idx_qrcode_expires_at", columnList = "expires_at"),
    @Index(name = "idx_qrcode_type", columnList = "qr_code_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class QRCode extends BaseEntity {

    @Column(name = "emv", length = 500, nullable = false, unique = true)
    private String emv;

    @Column(name = "payload", length = 2000, nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "qr_code_type", length = 20, nullable = false)
    private QRCodeType qrCodeType;

    @AttributeOverride(name = "amount", column = @Column(name = "amount", precision = 19, scale = 2))
    private Money amount;

    @Column(name = "description", length = 140)
    private String description;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "pix_key_id")
    private UUID pixKeyId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "max_usage", nullable = false)
    @Builder.Default
    private Integer maxUsage = 1;

    @Column(name = "image_base64", length = 10000)
    private String imageBase64;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !isExpired() && usageCount < maxUsage;
    }

    public void markUsed() {
        this.usageCount++;
        this.usedAt = OffsetDateTime.now();
    }
}