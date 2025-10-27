package com.paklog.wes.pack.events.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.shipping.shipment.ready.v1
 */
public record ShipmentReadyEvent(
    @JsonProperty("shipment_id") String shipmentId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("tracking_numbers") List<String> trackingNumbers,
    @JsonProperty("carton_count") int cartonCount,
    @JsonProperty("total_weight_kg") double totalWeightKg,
    @JsonProperty("carrier") String carrier,
    @JsonProperty("service_level") String serviceLevel,
    @JsonProperty("ready_at") Instant readyAt,
    @JsonProperty("pickup_scheduled_at") Instant pickupScheduledAt
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.shipping.shipment.ready.v1";
}
