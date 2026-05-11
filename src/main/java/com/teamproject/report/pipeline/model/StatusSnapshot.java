package com.teamproject.report.pipeline.model;

import com.teamproject.report.report.model.ReportStatus;

import java.time.Instant;

public record StatusSnapshot(
        String rawStage,
        PipelineStage currentStage,
        int searchCount,
        int summaryCount,
        int relevanceCount,
        Instant startedAt,
        Instant finishedAt,
        ReportStatus status,
        String failedStage,
        String message,
        String errorCode
) {
    public static StatusSnapshot pending() {
        return new StatusSnapshot(
                "started",
                PipelineStage.SEARCH,
                0,
                0,
                0,
                null,
                null,
                ReportStatus.PENDING,
                null,
                null,
                null
        );
    }
}
