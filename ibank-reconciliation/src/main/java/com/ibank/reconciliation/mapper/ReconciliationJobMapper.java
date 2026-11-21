package com.ibank.reconciliation.mapper;

import com.ibank.reconciliation.dto.ReconciliationJobResponse;
import com.ibank.reconciliation.model.ReconciliationJob;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ReconciliationJobMapper {

    ReconciliationJobMapper INSTANCE = Mappers.getMapper(ReconciliationJobMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "jobType", source = "jobType")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "sourceSystem", source = "sourceSystem")
    @Mapping(target = "targetSystem", source = "targetSystem")
    @Mapping(target = "externalReference", source = "externalReference")
    @Mapping(target = "parameters", source = "parameters")
    @Mapping(target = "totalRecords", source = "totalRecords")
    @Mapping(target = "matchedRecords", source = "matchedRecords")
    @Mapping(target = "mismatchedRecords", source = "mismatchedRecords")
    @Mapping(target = "missingInSource", source = "missingInSource")
    @Mapping(target = "missingInTarget", source = "missingInTarget")
    @Mapping(target = "breaksCount", source = "breaksCount")
    @Mapping(target = "resolvedBreaks", source = "resolvedBreaks")
    @Mapping(target = "startedAt", source = "startedAt")
    @Mapping(target = "completedAt", source = "completedAt")
    @Mapping(target = "errorMessage", source = "errorMessage")
    ReconciliationJobResponse toResponse(ReconciliationJob job);
}