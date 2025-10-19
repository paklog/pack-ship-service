package com.paklog.wes.pack.application.command;

import com.paklog.wes.pack.domain.entity.PackingInstruction;

import java.util.List;

/**
 * Command to start a new packing session
 */
public record StartPackingSessionCommand(
        String pickSessionId,
        String orderId,
        String workerId,
        String warehouseId,
        List<PackingInstruction> instructions
) {
}
