package com.ibank.card.mapper;

import com.ibank.card.controller.dto.CardRequest;
import com.ibank.card.controller.dto.CardResponse;
import com.ibank.card.model.Card;
import com.ibank.card.model.CardStatus;
import com.ibank.card.model.CardType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CardMapper {

    CardMapper INSTANCE = Mappers.getMapper(CardMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "numberHash", ignore = true)
    @Mapping(target = "lastFour", ignore = true)
    @Mapping(target = "maskedNumber", ignore = true)
    @Mapping(target = "cvvHash", ignore = true)
    @Mapping(target = "pinHash", ignore = true)
    @Mapping(target = "availableLimit", ignore = true)
    @Mapping(target = "dailyUsed", ignore = true)
    @Mapping(target = "monthlyUsed", ignore = true)
    @Mapping(target = "lastDailyReset", ignore = true)
    @Mapping(target = "lastMonthlyReset", ignore = true)
    @Mapping(target = "pinTries", ignore = true)
    @Mapping(target = "tokenId", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "closedReason", ignore = true)
    @Mapping(target = "status", constant = "PENDING_ACTIVATION")
    Card toEntity(CardRequest request);

    @Mapping(target = "type", source = "type")
    @Mapping(target = "status", source = "status")
    CardResponse toResponse(Card card);

    @Named("maskNumber")
    static String maskNumber(String number) {
        if (number == null || number.length() < 4) return "****";
        String lastFour = number.substring(number.length() - 4);
        return "**** **** **** " + lastFour;
    }
}