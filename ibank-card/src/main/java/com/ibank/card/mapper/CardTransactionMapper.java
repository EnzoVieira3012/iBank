package com.ibank.card.mapper;

import com.ibank.card.controller.dto.CardTransactionRequest;
import com.ibank.card.controller.dto.CardTransactionResponse;
import com.ibank.card.model.CardTransaction;
import com.ibank.card.model.TransactionStatus;
import com.ibank.card.model.TransactionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CardTransactionMapper {

    CardTransactionMapper INSTANCE = Mappers.getMapper(CardTransactionMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "settledAt", ignore = true)
    @Mapping(target = "nsu", ignore = true)
    @Mapping(target = "tid", ignore = true)
    @Mapping(target = "authorizationCode", ignore = true)
    CardTransaction toEntity(CardTransactionRequest request);

    @Mapping(target = "type", source = "type")
    @Mapping(target = "status", source = "status")
    CardTransactionResponse toResponse(CardTransaction transaction);
}