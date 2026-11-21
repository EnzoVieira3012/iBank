package com.ibank.fee.mapper;

import com.ibank.fee.controller.dto.FeeConfigRequest;
import com.ibank.fee.controller.dto.FeeConfigResponse;
import com.ibank.fee.model.FeeConfig;
import com.ibank.fee.model.FeeCalculationType;
import com.ibank.fee.model.FeePeriod;
import com.ibank.fee.model.FeeType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface FeeConfigMapper {

    FeeConfigMapper INSTANCE = Mappers.getMapper(FeeConfigMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    FeeConfig toEntity(FeeConfigRequest request);

    @Mapping(target = "feeType", source = "feeType")
    @Mapping(target = "period", source = "period")
    @Mapping(target = "calculationType", source = "calculationType")
    FeeConfigResponse toResponse(FeeConfig config);
}