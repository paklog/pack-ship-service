package com.paklog.wes.pack.domain.event;

import com.paklog.wes.pack.domain.shared.DomainEvent;
import java.time.Duration;

public class PackingSessionCompletedEvent extends DomainEvent {
    private final String sessionId;
    private final String orderId;
    private final String workerId;
    private final String warehouseId;
    private final int totalInstructions;
    private final int packedCount;
    private final int containerCount;
    private final double totalWeightLb;
    private final double accuracy;
    private final Duration duration;

    public PackingSessionCompletedEvent(String sessionId, String orderId, String workerId,
                                       String warehouseId, int totalInstructions, int packedCount,
                                       int containerCount, double totalWeightLb, double accuracy,
                                       Duration duration) {
        super();
        this.sessionId = sessionId;
        this.orderId = orderId;
        this.workerId = workerId;
        this.warehouseId = warehouseId;
        this.totalInstructions = totalInstructions;
        this.packedCount = packedCount;
        this.containerCount = containerCount;
        this.totalWeightLb = totalWeightLb;
        this.accuracy = accuracy;
        this.duration = duration;
    }

    public String getSessionId() { return sessionId; }
    public String getOrderId() { return orderId; }
    public String getWorkerId() { return workerId; }
    public String getWarehouseId() { return warehouseId; }
    public int getTotalInstructions() { return totalInstructions; }
    public int getPackedCount() { return packedCount; }
    public int getContainerCount() { return containerCount; }
    public double getTotalWeightLb() { return totalWeightLb; }
    public double getAccuracy() { return accuracy; }
    public Duration getDuration() { return duration; }
}
