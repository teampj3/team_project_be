package com.teamproject.report.pipeline.model;

import java.time.Instant;
import java.util.UUID;

public record PipelineRunMetadata(
        String runId,
        UUID reportId,
        String topic,
        Instant createdAt
) {
}
