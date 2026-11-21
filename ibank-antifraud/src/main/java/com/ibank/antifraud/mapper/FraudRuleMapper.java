package com.ibank.antifraud.mapper;

import com.ibank.antifraud.controller.dto.FraudRuleRequest;
import com.ibank.antifraud.controller.dto.FraudRuleResponse;
import com.ibank.antifraud.model.FraudRule;
import com.ibank.antifraud.model.FraudRuleAction;
import com.ibank.antifraud.model.FraudRuleType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface FraudRuleMapper {

    FraudRuleMapper INSTANCE = Mappers.getMapper(FraudRuleMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    FraudRule toEntity(FraudRuleRequest request);

    @Mapping(target = "ruleType", source = "ruleType")
    @Mapping(target = "action", source = "action")
    FraudRuleResponse toResponse(FraudRule rule);
}