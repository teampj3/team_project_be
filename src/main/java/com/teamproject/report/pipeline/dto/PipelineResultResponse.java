package com.teamproject.report.pipeline.dto;

import com.fasterxml.jackson.databind.JsonNode;
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
        VisualizationInfoResponse visualization,
        Instant startedAt,
        Instant finishedAt,
        ReportStatus status,
        String message,
        String errorCode,
        PipelineMetadataResponse pipelineMetadata,
        JsonNode reviewWriterLoop
) {
}
