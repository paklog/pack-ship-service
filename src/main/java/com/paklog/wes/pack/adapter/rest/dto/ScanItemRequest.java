package com.paklog.wes.pack.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO to scan an item
 */
public record ScanItemRequest(
        @NotBlank String barcode
) {
}
