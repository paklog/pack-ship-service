package com.paklog.wes.pack.adapter.rest.dto;

import com.paklog.wes.pack.domain.aggregate.PackingSession;
import com.paklog.wes.pack.domain.valueobject.PackingStatus;

/**
 * Response DTO for packing session
 */
public record PackingSessionResponse(
        String sessionId,
        String pickSessionId,
        String orderId,
        String workerId,
        String warehouseId,
        PackingStatus status,
        int totalInstructions,
        int packedCount,
        int containerCount,
        double progress,
        double accuracy
) {
    public static PackingSessionResponse from(PackingSession session) {
        return new PackingSessionResponse(
                session.getSessionId(),
                session.getPickSessionId(),
                session.getOrderId(),
                session.getWorkerId(),
                session.getWarehouseId(),
                session.getStatus(),
                session.getPackingInstructions().size(),
                session.getPackedInstructionCount(),
                session.getContainers().size(),
                session.getProgress(),
                session.calculateAccuracy()
        );
    }
}
