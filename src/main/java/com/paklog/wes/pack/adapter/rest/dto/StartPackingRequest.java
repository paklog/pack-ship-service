package com.paklog.wes.pack.adapter.rest.dto;

import com.paklog.wes.pack.domain.entity.PackingInstruction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO to start packing session
 */
public record StartPackingRequest(
        @NotBlank String pickSessionId,
        @NotBlank String orderId,
        @NotBlank String workerId,
        @NotBlank String warehouseId,
        @NotEmpty List<PackingInstruction> instructions
) {
}
