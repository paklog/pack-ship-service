package com.paklog.wes.pack.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO to select a carton
 */
public record SelectCartonRequest(
        @NotBlank String cartonType
) {
}
