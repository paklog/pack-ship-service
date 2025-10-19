package com.paklog.wes.pack.application.command;

import com.paklog.wes.pack.domain.entity.QualityCheck;

import java.util.List;

/**
 * Command to perform quality check on packing session
 */
public record PerformQualityCheckCommand(
        String sessionId,
        String checkerId,
        List<QualityCheck.Checkpoint> checkpoints,
        List<String> photoUrls
) {
}
