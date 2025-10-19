package com.paklog.wes.pack.adapter.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for quality check
 */
public record QualityCheckRequest(
        @NotBlank String checkerId,
        @NotEmpty @Valid List<CheckpointRequest> checkpoints,
        List<String> photoUrls
) {
    public record CheckpointRequest(
            @NotBlank String name,
            boolean passed,
            String notes
    ) {
    }
}
