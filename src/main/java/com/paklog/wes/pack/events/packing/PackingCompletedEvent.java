package com.paklog.wes.pack.events.packing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.packing.pack-task.completed.v1
 */
public record PackingCompletedEvent(
    @JsonProperty("pack_task_id") String packTaskId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("duration_seconds") long durationSeconds,
    @JsonProperty("items_packed") int itemsPacked,
    @JsonProperty("cartons_used") int cartonsUsed,
    @JsonProperty("total_weight_kg") double totalWeightKg,
    @JsonProperty("total_volume_cubic_meters") double totalVolumeCubicMeters
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.packing.pack-task.completed.v1";
}
