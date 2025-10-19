package com.paklog.wes.pack.application.command;

import com.paklog.wes.pack.domain.valueobject.Weight;

/**
 * Command to weigh and close a package
 */
public record WeighPackageCommand(
        String sessionId,
        Weight actualWeight
) {
}
