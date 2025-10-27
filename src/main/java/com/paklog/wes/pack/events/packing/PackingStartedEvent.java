package com.paklog.wes.pack.events.packing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.packing.pack-task.started.v1
 */
public record PackingStartedEvent(
    @JsonProperty("pack_task_id") String packTaskId,
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("items_to_pack") int itemsToPack,
    @JsonProperty("started_at") Instant startedAt,
    @JsonProperty("packing_station") String packingStation
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.packing.pack-task.started.v1";
}
