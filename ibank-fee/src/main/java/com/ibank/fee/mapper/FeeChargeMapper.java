package com.ibank.fee.mapper;

import com.ibank.fee.controller.dto.FeeChargeRequest;
import com.ibank.fee.controller.dto.FeeChargeResponse;
import com.ibank.fee.model.FeeCharge;
import com.ibank.fee.model.FeeChargeStatus;
import com.ibank.fee.model.FeeType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface FeeChargeMapper {

    FeeChargeMapper INSTANCE = Mappers.getMapper(FeeChargeMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "paymentTransactionId", ignore = true)
    FeeCharge toEntity(FeeChargeRequest request);

    @Mapping(target = "feeType", source = "feeType")
    @Mapping(target = "status", source = "status")
    FeeChargeResponse toResponse(FeeCharge charge);
}