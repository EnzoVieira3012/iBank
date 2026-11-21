package com.ibank.limit.mapper;

import com.ibank.limit.controller.dto.CustomerLimitRequest;
import com.ibank.limit.controller.dto.CustomerLimitResponse;
import com.ibank.limit.model.CustomerLimit;
import com.ibank.limit.model.LimitScope;
import com.ibank.limit.model.LimitType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CustomerLimitMapper {

    CustomerLimitMapper INSTANCE = Mappers.getMapper(CustomerLimitMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "currentUsage", ignore = true)
    @Mapping(target = "periodStart", ignore = true)
    @Mapping(target = "periodEnd", ignore = true)
    @Mapping(target = "active", constant = "true")
    CustomerLimit toEntity(CustomerLimitRequest request);

    @Mapping(target = "limitType", source = "limitType")
    @Mapping(target = "scope", source = "scope")
    CustomerLimitResponse toResponse(CustomerLimit limit);
}