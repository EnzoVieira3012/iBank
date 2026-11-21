package com.ibank.limit.mapper;

import com.ibank.limit.controller.dto.TransactionBlockRequest;
import com.ibank.limit.controller.dto.TransactionBlockResponse;
import com.ibank.limit.model.BlockScope;
import com.ibank.limit.model.BlockStatus;
import com.ibank.limit.model.BlockType;
import com.ibank.limit.model.TransactionBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TransactionBlockMapper {

    TransactionBlockMapper INSTANCE = Mappers.getMapper(TransactionBlockMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "revokedBy", ignore = true)
    @Mapping(target = "revokedAt", ignore = true)
    @Mapping(target = "revokeReason", ignore = true)
    TransactionBlock toEntity(TransactionBlockRequest request);

    @Mapping(target = "blockType", source = "blockType")
    @Mapping(target = "scope", source = "scope")
    @Mapping(target = "status", source = "status")
    TransactionBlockResponse toResponse(TransactionBlock block);
}