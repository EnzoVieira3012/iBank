package com.ibank.limit.model;

import com.ibank.core.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_blocks", indexes = {
    @Index(name = "idx_blocks_customer", columnList = "customer_id"),
    @Index(name = "idx_blocks_account", columnList = "account_id"),
    @Index(name = "idx_blocks_card", columnList = "card_id"),
    @Index(name = "idx_blocks_type", columnList = "block_type"),
    @Index(name = "idx_blocks_status", columnList = "status"),
    @Index(name = "idx_blocks_scope", columnList = "scope"),
    @Index(name = "idx_blocks_dates", columnList = "starts_at,expires_at"),
    @Index(name = "idx_blocks_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionBlock extends BaseEntity {

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "card_id")
    private UUID cardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 30)
    private BlockType blockType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 30)
    private BlockScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BlockStatus status;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoke_reason", length = 500)
    private String revokeReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    private String conditions;

    @Column(name = "affected_channels", length = 200)
    private String affectedChannels;

    @Column(name = "affected_transaction_types", length = 200)
    private String affectedTransactionTypes;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Version
    private Long version;
}