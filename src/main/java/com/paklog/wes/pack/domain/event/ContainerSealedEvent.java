package com.paklog.wes.pack.domain.event;

import com.paklog.domain.shared.DomainEvent;

public class ContainerSealedEvent extends DomainEvent {
    private final String sessionId;
    private final String containerId;
    private final int itemCount;
    private final double weightLb;
    private final String workerId;

    public ContainerSealedEvent(String sessionId, String containerId, int itemCount,
                               double weightLb, String workerId) {
        super();
        this.sessionId = sessionId;
        this.containerId = containerId;
        this.itemCount = itemCount;
        this.weightLb = weightLb;
        this.workerId = workerId;
    }

    public String getSessionId() { return sessionId; }
    public String getContainerId() { return containerId; }
    public int getItemCount() { return itemCount; }
    public double getWeightLb() { return weightLb; }
    public String getWorkerId() { return workerId; }
}
