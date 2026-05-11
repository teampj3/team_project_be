package com.teamproject.report.pipeline.dto;

import com.teamproject.report.report.model.ReportStatus;

public record PipelineRunResponse(
        String runId,
        String topic,
        ReportStatus status,
        String currentStage,
        String message,
        String errorCode
) {
}
