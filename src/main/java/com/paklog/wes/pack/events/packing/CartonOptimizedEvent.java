package com.paklog.wes.pack.events.packing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.packing.carton.optimized.v1
 */
public record CartonOptimizedEvent(
    @JsonProperty("optimization_id") String optimizationId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("items_count") int itemsCount,
    @JsonProperty("cartons_required") int cartonsRequired,
    @JsonProperty("total_volume_cubic_meters") double totalVolumeCubicMeters,
    @JsonProperty("space_utilization_percentage") double spaceUtilizationPercentage,
    @JsonProperty("algorithm_used") String algorithmUsed,
    @JsonProperty("optimization_time_ms") long optimizationTimeMs,
    @JsonProperty("optimized_at") Instant optimizedAt
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.packing.carton.optimized.v1";
}
