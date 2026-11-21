package com.ibank.reconciliation.dto;

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
@Schema(description = "Reconciliation rule response")
public class ReconciliationRuleResponse {

    @Schema(description = "Rule UUID")
    private UUID id;

    @Schema(description = "Rule name")
    private String name;

    @Schema(description = "Rule description")
    private String description;

    @Schema(description = "Job type this rule applies to")
    private String jobType;

    @Schema(description = "Entity type this rule applies to")
    private String entityType;

    @Schema(description = "Rule priority (higher = evaluated first)")
    private Integer priority;

    @Schema(description = "Whether rule is active")
    private Boolean active;

    @Schema(description = "Match keys configuration")
    private Map<String, Object> matchKeys;

    @Schema(description = "Fields to compare")
    private Map<String, Object> compareFields;

    @Schema(description = "Tolerance configuration")
    private Map<String, Object> toleranceConfig;

    @Schema(description = "Break type mapping")
    private Map<String, Object> breakTypeMapping;

    @Schema(description = "Created at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @Schema(description = "Updated at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;
}