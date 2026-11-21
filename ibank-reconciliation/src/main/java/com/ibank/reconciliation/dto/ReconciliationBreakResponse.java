package com.ibank.reconciliation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reconciliation break response")
public class ReconciliationBreakResponse {

    @Schema(description = "Break UUID")
    private UUID id;

    @Schema(description = "Parent job UUID")
    private UUID jobId;

    @Schema(description = "Break type")
    private String breakType;

    @Schema(description = "Entity type")
    private String entityType;

    @Schema(description = "Entity ID")
    private String entityId;

    @Schema(description = "Source reference")
    private String sourceReference;

    @Schema(description = "Target reference")
    private String targetReference;

    @Schema(description = "Source amount")
    private BigDecimal sourceAmount;

    @Schema(description = "Target amount")
    private BigDecimal targetAmount;

    @Schema(description = "Difference amount")
    private BigDecimal differenceAmount;

    @Schema(description = "Currency")
    private String currency;

    @Schema(description = "Break status")
    private String status;

    @Schema(description = "Priority")
    private String priority;

    @Schema(description = "Assignee ID")
    private String assigneeId;

    @Schema(description = "Source data")
    private Map<String, Object> sourceData;

    @Schema(description = "Target data")
    private Map<String, Object> targetData;

    @Schema(description = "Resolution data")
    private Map<String, Object> resolutionData;

    @Schema(description = "Resolution notes")
    private String resolutionNotes;

    @Schema(description = "Resolved by")
    private String resolvedBy;

    @Schema(description = "Created at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @Schema(description = "Resolved at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant resolvedAt;

    @Schema(description = "Due date")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant dueDate;
}