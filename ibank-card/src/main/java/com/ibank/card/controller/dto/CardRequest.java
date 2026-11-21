package com.ibank.card.controller.dto;

import com.ibank.card.model.CardType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardRequest {

    @NotNull(message = "Account ID is required")
    private String accountId;

    @NotNull(message = "Card type is required")
    private CardType type;

    @NotBlank(message = "Holder name is required")
    @Size(max = 100, message = "Holder name must not exceed 100 characters")
    private String holderName;

    @NotNull(message = "Expiry date is required")
    private String expiryDate;

    @NotBlank(message = "CVV is required")
    @Size(min = 3, max = 4, message = "CVV must be 3 or 4 digits")
    private String cvv;

    @Size(min = 4, max = 6, message = "PIN must be 4-6 digits")
    private String pin;

    private BigDecimal creditLimit;

    private BigDecimal dailyLimit;

    private BigDecimal monthlyLimit;

    private Boolean contactlessEnabled;

    private Boolean onlineEnabled;

    private Boolean internationalEnabled;
}