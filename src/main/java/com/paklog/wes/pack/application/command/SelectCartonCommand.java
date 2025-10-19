package com.paklog.wes.pack.application.command;

/**
 * Command to select a carton for packing session
 */
public record SelectCartonCommand(
        String sessionId,
        String cartonType
) {
}
