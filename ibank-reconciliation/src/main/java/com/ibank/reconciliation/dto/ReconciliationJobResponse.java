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
@Schema(description = "Reconciliation job response")
public class ReconciliationJobResponse {

    @Schema(description = "Job UUID")
    private UUID id;

    @Schema(description = "Job type")
    private String jobType;

    @Schema(description = "Job status")
    private String status;

    @Schema(description = "Source system name")
    private String sourceSystem;

    @Schema(description = "Target system name")
    private String targetSystem;

    @Schema(description = "External reference")
    private String externalReference;

    @Schema(description = "Job parameters")
    private Map<String, Object> parameters;

    @Schema(description = "Total records processed")
    private Long totalRecords;

    @Schema(description = "Matched records count")
    private Long matchedRecords;

    @Schema(description = "Mismatched records count")
    private Long mismatchedRecords;

    @Schema(description = "Missing in source count")
    private Long missingInSource;

    @Schema(description = "Missing in target count")
    private Long missingInTarget;

    @Schema(description = "Breaks detected")
    private Long breaksCount;

    @Schema(description = "Breaks resolved")
    private Long resolvedBreaks;

    @Schema(description = "Job started at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startedAt;

    @Schema(description = "Job completed at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant completedAt;

    @Schema(description = "Error message if failed")
    private String errorMessage;
}