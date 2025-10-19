package com.paklog.wes.pack.application.command;

import com.paklog.wes.pack.domain.valueobject.Address;

/**
 * Command to generate shipping label
 */
public record GenerateLabelCommand(
        String shipmentId,
        Address fromAddress
) {
}
