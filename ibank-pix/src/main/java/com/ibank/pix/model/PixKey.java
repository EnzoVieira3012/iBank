package com.ibank.pix.model;

import com.ibank.core.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pix_keys", indexes = {
    @Index(name = "idx_pix_keys_key_value", columnList = "key_value", unique = true),
    @Index(name = "idx_pix_keys_account_id", columnList = "account_id"),
    @Index(name = "idx_pix_keys_holder_cpf", columnList = "holder_cpf"),
    @Index(name = "idx_pix_keys_status", columnList = "status"),
    @Index(name = "idx_pix_keys_type", columnList = "key_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PixKey extends BaseEntity {

    @Column(name = "key_value", length = 100, nullable = false, unique = true)
    private String keyValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", length = 20, nullable = false)
    private PixKeyType keyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private PixKeyStatus status = PixKeyStatus.PENDING;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "holder_name", length = 255, nullable = false)
    private String holderName;

    @Column(name = "holder_cpf", length = 18, nullable = false)
    private String holderCpf;

    @Column(name = "holder_account_type", length = 20, nullable = false)
    private String holderAccountType;

    @Column(name = "holder_branch", length = 10)
    private String holderBranch;

    @Column(name = "holder_account_number", length = 20)
    private String holderAccountNumber;

    @Column(name = "dict_registration_id", length = 100)
    private String dictRegistrationId;

    @Column(name = "dict_registered_at")
    private OffsetDateTime dictRegisteredAt;

    @Column(name = "claimed_at")
    private OffsetDateTime claimedAt;

    @Column(name = "claimed_by_cpf", length = 18)
    private String claimedByCpf;

    @Column(name = "blocked_at")
    private OffsetDateTime blockedAt;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    public boolean isActive() {
        return status == PixKeyStatus.ACTIVE;
    }

    public boolean canBeUsed() {
        return status == PixKeyStatus.ACTIVE;
    }
}