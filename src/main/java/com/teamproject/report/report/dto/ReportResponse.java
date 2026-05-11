package com.teamproject.report.report.dto;

import com.teamproject.report.report.model.Report;
import com.teamproject.report.report.model.ReportStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        String topic,
        ReportStatus status,
        String gptDraft,
        String claudeDraft,
        List<String> commonHighlights,
        List<String> differentHighlights,
        String reviewResult,
        String mergedReport,
        String failureMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getTopic(),
                report.getStatus(),
                report.getGptDraft(),
                report.getClaudeDraft(),
                report.getCommonHighlights(),
                report.getDifferentHighlights(),
                report.getReviewResult(),
                report.getMergedReport(),
                report.getFailureMessage(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
