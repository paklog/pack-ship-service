package com.paklog.wes.pack.adapter.rest.dto;

import jakarta.validation.constraints.Positive;

/**
 * Request DTO to weigh a package
 */
public record WeighPackageRequest(
        @Positive double weightLb
) {
}
