package com.teamproject.report.pipeline.service;

import com.teamproject.report.pipeline.model.PipelineRunMetadata;
import com.teamproject.report.pipeline.model.StatusSnapshot;
import com.teamproject.report.report.dto.AiReportResponse;
import com.teamproject.report.report.dto.ReportResponse;
import com.teamproject.report.report.model.ReportStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PipelineReportService {

    private final PipelineService pipelineService;
    private final PipelineFileService pipelineFileService;

    public PipelineReportService(PipelineService pipelineService, PipelineFileService pipelineFileService) {
        this.pipelineService = pipelineService;
        this.pipelineFileService = pipelineFileService;
    }

    public ReportResponse getLatest() {
        return build(pipelineService.getLatestRun());
    }

    public ReportResponse getByReportId(UUID reportId) {
        return build(pipelineService.getRunByReportId(reportId));
    }

    private ReportResponse build(PipelineRunMetadata metadata) {
        AiReportResponse writerOutput = pipelineFileService.readWriterOutput(metadata.runId());
        StatusSnapshot status = pipelineFileService.readStatus(metadata.runId());
        Instant startedAt = status.startedAt() == null ? metadata.createdAt() : status.startedAt();
        Instant updatedAt = status.finishedAt() == null ? startedAt : status.finishedAt();

        return new ReportResponse(
                metadata.reportId(),
                metadata.topic(),
                mapStatus(status.status()),
                writerOutput.gptDraft(),
                writerOutput.claudeDraft(),
                writerOutput.commonHighlights(),
                writerOutput.differentHighlights(),
                writerOutput.reviewResult(),
                writerOutput.mergedReport(),
                status.message(),
                startedAt,
                updatedAt
        );
    }

    private ReportStatus mapStatus(ReportStatus status) {
        return status;
    }
}
