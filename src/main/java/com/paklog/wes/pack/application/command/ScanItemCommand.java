package com.paklog.wes.pack.application.command;

/**
 * Command to scan an item in packing session
 */
public record ScanItemCommand(
        String sessionId,
        String barcode
) {
}
