package com.paklog.wes.pack.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Contract for PickingCompletedEvent from pick-execution-service
 * Anti-Corruption Layer
 */
public record PickingCompletedContract(
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("items_picked") int itemsPicked
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pick-execution.picking.pick-task.completed.v1";
}
