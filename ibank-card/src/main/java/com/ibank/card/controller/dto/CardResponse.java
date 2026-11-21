package com.ibank.card.controller.dto;

import com.ibank.card.model.CardStatus;
import com.ibank.card.model.CardType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CardResponse {

    private UUID id;
    private UUID accountId;
    private String maskedNumber;
    private String lastFour;
    private CardType type;
    private CardStatus status;
    private String holderName;
    private LocalDate expiryDate;
    private BigDecimal creditLimit;
    private BigDecimal availableLimit;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal dailyUsed;
    private BigDecimal monthlyUsed;
    private Boolean contactlessEnabled;
    private Boolean onlineEnabled;
    private Boolean internationalEnabled;
    private String tokenId;
    private LocalDateTime closedAt;
    private String closedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}