package com.ibank.reconciliation.mapper;

import com.ibank.reconciliation.dto.ReconciliationRuleResponse;
import com.ibank.reconciliation.model.ReconciliationRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ReconciliationRuleMapper {

    ReconciliationRuleMapper INSTANCE = Mappers.getMapper(ReconciliationRuleMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "jobType", source = "jobType")
    @Mapping(target = "entityType", source = "entityType")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "matchKeys", source = "matchKeys")
    @Mapping(target = "compareFields", source = "compareFields")
    @Mapping(target = "toleranceConfig", source = "toleranceConfig")
    @Mapping(target = "breakTypeMapping", source = "breakTypeMapping")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    ReconciliationRuleResponse toResponse(ReconciliationRule rule);
}