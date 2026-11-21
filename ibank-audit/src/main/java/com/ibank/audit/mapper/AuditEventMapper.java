package com.ibank.audit.mapper;

import com.ibank.audit.dto.AuditEventResponse;
import com.ibank.audit.model.AuditEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AuditEventMapper {

    AuditEventMapper INSTANCE = Mappers.getMapper(AuditEventMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "eventType", source = "eventType")
    @Mapping(target = "entityType", source = "entityType")
    @Mapping(target = "entityId", source = "entityId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "correlationId", source = "correlationId")
    @Mapping(target = "causationId", source = "causationId")
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "metadata", source = "metadata")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "userAgent", source = "userAgent")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "checksum", source = "checksum")
    AuditEventResponse toResponse(AuditEvent event);
}