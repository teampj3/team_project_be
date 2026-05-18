package com.teamproject.report.archive.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamproject.report.archive.model.ArchiveEntry;
import com.teamproject.report.report.model.ReportStatus;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArchiveResponse(
        UUID archiveId,
        UUID reportId,
        String runId,
        String title,
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static ArchiveResponse from(ArchiveEntry entry) {
        return new ArchiveResponse(
                entry.getId(),
                entry.getReportId(),
                entry.getRunId(),
                entry.getTitle(),
                entry.getTopic(),
                entry.getStatus(),
                entry.getGptDraft(),
                entry.getClaudeDraft(),
                readList(entry.getCommonHighlightsJson()),
                readList(entry.getDifferentHighlightsJson()),
                entry.getReviewResult(),
                entry.getMergedReport(),
                entry.getFailureMessage(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private static List<String> readList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(raw, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse archive highlights", e);
        }
    }
}
