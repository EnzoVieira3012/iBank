package com.ibank.antifraud.mapper;

import com.ibank.antifraud.controller.dto.RiskScoreResponse;
import com.ibank.antifraud.model.RiskLevel;
import com.ibank.antifraud.model.RiskScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RiskScoreMapper {

    RiskScoreMapper INSTANCE = Mappers.getMapper(RiskScoreMapper.class);

    @Mapping(target = "riskLevel", source = "riskLevel")
    RiskScoreResponse toResponse(RiskScore score);
}