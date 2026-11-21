package com.ibank.reconciliation.mapper;

import com.ibank.reconciliation.dto.ReconciliationBreakResponse;
import com.ibank.reconciliation.model.ReconciliationBreak;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ReconciliationBreakMapper {

    ReconciliationBreakMapper INSTANCE = Mappers.getMapper(ReconciliationBreakMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "breakType", source = "breakType")
    @Mapping(target = "entityType", source = "entityType")
    @Mapping(target = "entityId", source = "entityId")
    @Mapping(target = "sourceReference", source = "sourceReference")
    @Mapping(target = "targetReference", source = "targetReference")
    @Mapping(target = "sourceAmount", source = "sourceAmount")
    @Mapping(target = "targetAmount", source = "targetAmount")
    @Mapping(target = "differenceAmount", source = "differenceAmount")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "assigneeId", source = "assigneeId")
    @Mapping(target = "sourceData", source = "sourceData")
    @Mapping(target = "targetData", source = "targetData")
    @Mapping(target = "resolutionData", source = "resolutionData")
    @Mapping(target = "resolutionNotes", source = "resolutionNotes")
    @Mapping(target = "resolvedBy", source = "resolvedBy")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "resolvedAt", source = "resolvedAt")
    @Mapping(target = "dueDate", source = "dueDate")
    ReconciliationBreakResponse toResponse(ReconciliationBreak brk);
}