package com.paklog.wes.pack.domain.event;

import com.paklog.wes.pack.domain.shared.DomainEvent;

/**
 * Event published when packing session starts
 */
public class PackingSessionStartedEvent extends DomainEvent {

    private final String sessionId;
    private final String pickSessionId;
    private final String orderId;
    private final String workerId;
    private final String warehouseId;
    private final int totalInstructions;

    public PackingSessionStartedEvent(
            String sessionId,
            String pickSessionId,
            String orderId,
            String workerId,
            String warehouseId,
            int totalInstructions
    ) {
        super();
        this.sessionId = sessionId;
        this.pickSessionId = pickSessionId;
        this.orderId = orderId;
        this.workerId = workerId;
        this.warehouseId = warehouseId;
        this.totalInstructions = totalInstructions;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getPickSessionId() {
        return pickSessionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public int getTotalInstructions() {
        return totalInstructions;
    }
}
