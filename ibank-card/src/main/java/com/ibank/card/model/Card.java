package com.ibank.card.model;

import com.ibank.core.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_cards_account_id", columnList = "account_id"),
    @Index(name = "idx_cards_number_hash", columnList = "number_hash"),
    @Index(name = "idx_cards_status", columnList = "status"),
    @Index(name = "idx_cards_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "number_hash", nullable = false, length = 64)
    private String numberHash;

    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;

    @Column(name = "masked_number", nullable = false, length = 19)
    private String maskedNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CardType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status;

    @Column(name = "holder_name", nullable = false, length = 100)
    private String holderName;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "cvv_hash", nullable = false, length = 64)
    private String cvvHash;

    @Column(name = "credit_limit", precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "available_limit", precision = 19, scale = 2)
    private BigDecimal availableLimit;

    @Column(name = "daily_limit", precision = 19, scale = 2)
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit", precision = 19, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(name = "daily_used", precision = 19, scale = 2)
    private BigDecimal dailyUsed;

    @Column(name = "monthly_used", precision = 19, scale = 2)
    private BigDecimal monthlyUsed;

    @Column(name = "last_daily_reset")
    private LocalDate lastDailyReset;

    @Column(name = "last_monthly_reset")
    private LocalDate lastMonthlyReset;

    @Column(name = "pin_hash", length = 64)
    private String pinHash;

    @Column(name = "pin_tries")
    private Integer pinTries;

    @Column(name = "contactless_enabled")
    private Boolean contactlessEnabled;

    @Column(name = "online_enabled")
    private Boolean onlineEnabled;

    @Column(name = "international_enabled")
    private Boolean internationalEnabled;

    @Column(name = "token_id", length = 100)
    private String tokenId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_reason", length = 255)
    private String closedReason;

    @Version
    private Long version;
}