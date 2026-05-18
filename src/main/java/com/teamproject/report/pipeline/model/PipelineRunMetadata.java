package com.teamproject.report.pipeline.model;

import java.time.Instant;
import java.util.UUID;

public record PipelineRunMetadata(
        String runId,
        UUID reportId,
        UUID userId,
        String topic,
        Instant createdAt
) {
}
