package com.paklog.wes.pack.application.command;

/**
 * Command to pack an item into a container
 */
public record PackItemCommand(
        String sessionId,
        String instructionId,
        String containerId,
        int quantity
) {
}
