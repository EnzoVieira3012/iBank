package com.ibank.antifraud.mapper;

import com.ibank.antifraud.controller.dto.FraudCaseRequest;
import com.ibank.antifraud.controller.dto.FraudCaseResponse;
import com.ibank.antifraud.model.CaseStatus;
import com.ibank.antifraud.model.FraudCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface FraudCaseMapper {

    FraudCaseMapper INSTANCE = Mappers.getMapper(FraudCaseMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "caseNumber", ignore = true)
    @Mapping(target = "status", constant = "OPEN")
    @Mapping(target = "sarFiled", constant = "false")
    FraudCase toEntity(FraudCaseRequest request);

    @Mapping(target = "status", source = "status")
    FraudCaseResponse toResponse(FraudCase fraudCase);
}