package com.ibank.antifraud.mapper;

import com.ibank.antifraud.controller.dto.FraudAlertResponse;
import com.ibank.antifraud.model.AlertSeverity;
import com.ibank.antifraud.model.AlertStatus;
import com.ibank.antifraud.model.FraudAlert;
import com.ibank.antifraud.model.RiskLevel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface FraudAlertMapper {

    FraudAlertMapper INSTANCE = Mappers.getMapper(FraudAlertMapper.class);

    @Mapping(target = "status", source = "status")
    @Mapping(target = "severity", source = "severity")
    @Mapping(target = "riskLevel", source = "riskLevel")
    FraudAlertResponse toResponse(FraudAlert alert);
}