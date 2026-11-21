package com.ibank.audit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit event response")
public class AuditEventResponse {

    @Schema(description = "Unique event identifier")
    private UUID id;

    @Schema(description = "Type of audit event")
    private String eventType;

    @Schema(description = "Type of entity being audited")
    private String entityType;

    @Schema(description = "ID of the entity being audited")
    private String entityId;

    @Schema(description = "User who triggered the event")
    private String userId;

    @Schema(description = "Session identifier")
    private String sessionId;

    @Schema(description = "Correlation ID for distributed tracing")
    private String correlationId;

    @Schema(description = "Causation ID for event chaining")
    private String causationId;

    @Schema(description = "Event payload")
    private Map<String, Object> payload;

    @Schema(description = "Additional metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Client IP address")
    private String ipAddress;

    @Schema(description = "Client user agent")
    private String userAgent;

    @Schema(description = "When the event occurred")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;

    @Schema(description = "Tamper-proof checksum")
    private String checksum;
}