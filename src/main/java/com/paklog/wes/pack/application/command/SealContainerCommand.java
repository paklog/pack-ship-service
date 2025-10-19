package com.paklog.wes.pack.application.command;

import com.paklog.wes.pack.domain.valueobject.Weight;

/**
 * Command to seal a container
 */
public record SealContainerCommand(
        String sessionId,
        String containerId,
        Weight actualWeight
) {
}
