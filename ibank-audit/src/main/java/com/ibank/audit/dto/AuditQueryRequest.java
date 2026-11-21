package com.ibank.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Schema(description = "Audit event query parameters")
public class AuditQueryRequest {

    @Schema(description = "Filter by entity type")
    private String entityType;

    @Schema(description = "Filter by entity ID")
    private String entityId;

    @Schema(description = "Filter by user ID")
    private String userId;

    @Schema(description = "Filter by correlation ID")
    private String correlationId;

    @Schema(description = "Filter by event types")
    private List<String> eventTypes;

    @Schema(description = "Start of time range (inclusive)")
    private Instant start;

    @Schema(description = "End of time range (inclusive)")
    private Instant end;

    @Schema(description = "Page number (0-based)", defaultValue = "0")
    private int page = 0;

    @Schema(description = "Page size", defaultValue = "20")
    private int size = 20;

    @Schema(description = "Sort field", defaultValue = "occurredAt")
    private String sort = "occurredAt";

    @Schema(description = "Sort direction", defaultValue = "DESC")
    private String direction = "DESC";
}