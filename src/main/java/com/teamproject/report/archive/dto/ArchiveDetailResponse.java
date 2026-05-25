package com.teamproject.report.archive.dto;

import com.teamproject.report.archive.model.ArchiveEntry;
import com.teamproject.report.pipeline.dto.PipelineResultResponse;
import com.teamproject.report.pipeline.dto.RelevancePaperResponse;
import com.teamproject.report.pipeline.dto.ReaderPaperResponse;
import com.teamproject.report.pipeline.dto.SearchPaperResponse;
import com.teamproject.report.pipeline.dto.VisualizationInfoResponse;
import com.teamproject.report.report.model.ReportStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArchiveDetailResponse(
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
        Instant updatedAt,
        PipelineResultResponse pipelineResult,
        VisualizationInfoResponse visualization,
        List<SearchPaperResponse> searchResults,
        List<ReaderPaperResponse> readerResults,
        List<RelevancePaperResponse> relevanceResults
) {

    public static ArchiveDetailResponse from(
            ArchiveEntry entry,
            PipelineResultResponse pipelineResult,
            VisualizationInfoResponse visualization,
            List<SearchPaperResponse> searchResults,
            List<ReaderPaperResponse> readerResults,
            List<RelevancePaperResponse> relevanceResults
    ) {
        ArchiveResponse base = ArchiveResponse.from(entry);
        return new ArchiveDetailResponse(
                base.archiveId(),
                base.reportId(),
                base.runId(),
                base.title(),
                base.topic(),
                base.status(),
                base.gptDraft(),
                base.claudeDraft(),
                base.commonHighlights(),
                base.differentHighlights(),
                base.reviewResult(),
                base.mergedReport(),
                base.failureMessage(),
                base.createdAt(),
                base.updatedAt(),
                pipelineResult,
                visualization,
                searchResults,
                readerResults,
                relevanceResults
        );
    }
}
