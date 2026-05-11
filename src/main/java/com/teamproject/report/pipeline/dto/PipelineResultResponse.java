package com.teamproject.report.pipeline.dto;

import com.teamproject.report.report.model.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record PipelineResultResponse(
        String runId,
        UUID reportId,
        String topic,
        String currentStage,
        int searchCount,
        int summaryCount,
        int relevanceCount,
        String reportPath,
        Instant startedAt,
        Instant finishedAt,
        ReportStatus status,
        String message,
        String errorCode
) {
}
