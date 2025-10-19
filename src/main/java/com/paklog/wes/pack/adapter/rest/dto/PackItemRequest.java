package com.paklog.wes.pack.adapter.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO to pack item
 */
public record PackItemRequest(
        @NotBlank String instructionId,
        String containerId, // Optional - will auto-select if null
        @Min(1) int quantity
) {
}
