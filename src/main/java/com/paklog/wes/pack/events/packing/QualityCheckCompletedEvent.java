package com.paklog.wes.pack.events.packing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.packing.quality-check.completed.v1
 */
public record QualityCheckCompletedEvent(
    @JsonProperty("quality_check_id") String qualityCheckId,
    @JsonProperty("pack_task_id") String packTaskId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("check_type") String checkType,
    @JsonProperty("result") String result,
    @JsonProperty("checked_by") String checkedBy,
    @JsonProperty("checked_at") Instant checkedAt,
    @JsonProperty("issues_found") List<String> issuesFound
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.packing.quality-check.completed.v1";
}
