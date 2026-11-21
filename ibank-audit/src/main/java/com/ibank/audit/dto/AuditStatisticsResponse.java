package com.ibank.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit statistics response")
public class AuditStatisticsResponse {

    @Schema(description = "Total events in period")
    private long totalEvents;

    @Schema(description = "Period start")
    private Instant periodStart;

    @Schema(description = "Period end")
    private Instant periodEnd;

    @Schema(description = "Events grouped by type")
    private List<EventTypeCount> eventTypeBreakdown;

    @Schema(description = "Events grouped by entity type")
    private Map<String, Long> entityTypeBreakdown;

    @Schema(description = "Top users by activity")
    private List<UserActivityCount> topUsers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventTypeCount {
        private String eventType;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityCount {
        private String userId;
        private long count;
    }
}