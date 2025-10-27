package com.paklog.wes.pack.domain.event;

import com.paklog.wes.pack.domain.shared.DomainEvent;

public class ItemPackedEvent extends DomainEvent {
    private final String sessionId;
    private final String instructionId;
    private final String itemSku;
    private final String containerId;
    private final int quantity;
    private final String workerId;

    public ItemPackedEvent(String sessionId, String instructionId, String itemSku,
                          String containerId, int quantity, String workerId) {
        super();
        this.sessionId = sessionId;
        this.instructionId = instructionId;
        this.itemSku = itemSku;
        this.containerId = containerId;
        this.quantity = quantity;
        this.workerId = workerId;
    }

    public String getSessionId() { return sessionId; }
    public String getInstructionId() { return instructionId; }
    public String getItemSku() { return itemSku; }
    public String getContainerId() { return containerId; }
    public int getQuantity() { return quantity; }
    public String getWorkerId() { return workerId; }
}
