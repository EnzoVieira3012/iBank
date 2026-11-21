package com.ibank.fee.mapper;

import com.ibank.fee.controller.dto.InterestRateRequest;
import com.ibank.fee.controller.dto.InterestRateResponse;
import com.ibank.fee.model.InterestIndex;
import com.ibank.fee.model.InterestRate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface InterestRateMapper {

    InterestRateMapper INSTANCE = Mappers.getMapper(InterestRateMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "effectiveRate", ignore = true)
    InterestRate toEntity(InterestRateRequest request);

    @Mapping(target = "indexType", source = "indexType")
    InterestRateResponse toResponse(InterestRate rate);
}