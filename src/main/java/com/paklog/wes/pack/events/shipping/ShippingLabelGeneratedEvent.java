package com.paklog.wes.pack.events.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.shipping.label.generated.v1
 */
public record ShippingLabelGeneratedEvent(
    @JsonProperty("label_id") String labelId,
    @JsonProperty("tracking_number") String trackingNumber,
    @JsonProperty("carton_id") String cartonId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("carrier") String carrier,
    @JsonProperty("service_level") String serviceLevel,
    @JsonProperty("generated_at") Instant generatedAt,
    @JsonProperty("estimated_delivery_date") LocalDate estimatedDeliveryDate
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.shipping.label.generated.v1";
}
