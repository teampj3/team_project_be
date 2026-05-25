package com.teamproject.report.archive.service;

import com.teamproject.report.archive.dto.ArchiveDetailResponse;
import com.teamproject.report.archive.dto.ArchiveResponse;
import com.teamproject.report.archive.dto.SaveArchiveRequest;
import com.teamproject.report.archive.exception.ArchiveNotFoundException;
import com.teamproject.report.archive.model.ArchiveEntry;
import com.teamproject.report.archive.repository.ArchiveEntryRepository;
import com.teamproject.report.auth.model.UserAccount;
import com.teamproject.report.auth.repository.UserAccountRepository;
import com.teamproject.report.auth.service.AuthService;
import com.teamproject.report.pipeline.exception.PipelineRunNotFoundException;
import com.teamproject.report.pipeline.dto.PipelineResultResponse;
import com.teamproject.report.pipeline.dto.RelevancePaperResponse;
import com.teamproject.report.pipeline.dto.ReaderPaperResponse;
import com.teamproject.report.pipeline.dto.SearchPaperResponse;
import com.teamproject.report.pipeline.dto.VisualizationInfoResponse;
import com.teamproject.report.pipeline.model.PipelineRunMetadata;
import com.teamproject.report.pipeline.model.StatusSnapshot;
import com.teamproject.report.pipeline.service.PipelineFileService;
import com.teamproject.report.pipeline.service.PipelineRunRegistry;
import com.teamproject.report.report.dto.AiReportResponse;
import com.teamproject.report.report.dto.ReportResponse;
import com.teamproject.report.report.model.ReportStatus;
import com.teamproject.report.report.service.ReportNotFoundException;
import com.teamproject.report.report.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ArchiveService {

    private final ArchiveEntryRepository archiveEntryRepository;
    private final AuthService authService;
    private final UserAccountRepository userAccountRepository;
    private final ReportService reportService;
    private final PipelineRunRegistry pipelineRunRegistry;
    private final PipelineFileService pipelineFileService;

    public ArchiveService(
            ArchiveEntryRepository archiveEntryRepository,
            AuthService authService,
            UserAccountRepository userAccountRepository,
            ReportService reportService,
            PipelineRunRegistry pipelineRunRegistry,
            PipelineFileService pipelineFileService
    ) {
        this.archiveEntryRepository = archiveEntryRepository;
        this.authService = authService;
        this.userAccountRepository = userAccountRepository;
        this.reportService = reportService;
        this.pipelineRunRegistry = pipelineRunRegistry;
        this.pipelineFileService = pipelineFileService;
    }

    @Transactional
    public ArchiveResponse save(String authorization, SaveArchiveRequest request) {
        UserAccount user = authService.requireCurrentUser(authorization);
        ReportResponse report = resolveReport(request.reportId());
        String runId = resolveRunId(request.reportId());
        String title = request.title() == null || request.title().isBlank() ? report.topic() : request.title().trim();

        ArchiveEntry entry = archiveEntryRepository.findByUserIdAndReportId(user.getId(), request.reportId())
                .map(existing -> {
                    existing.updateFrom(runId, title, report);
                    return existing;
                })
                .orElseGet(() -> new ArchiveEntry(user, request.reportId(), runId, title, report));

        return ArchiveResponse.from(archiveEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<ArchiveResponse> list(String authorization) {
        UserAccount user = authService.requireCurrentUser(authorization);
        return archiveEntryRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(ArchiveResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArchiveResponse get(String authorization, UUID archiveId) {
        UserAccount user = authService.requireCurrentUser(authorization);
        ArchiveEntry entry = archiveEntryRepository.findByIdAndUserId(archiveId, user.getId())
                .orElseThrow(() -> new ArchiveNotFoundException(archiveId));
        return ArchiveResponse.from(entry);
    }

    @Transactional(readOnly = true)
    public ArchiveDetailResponse getDetail(String authorization, UUID archiveId) {
        UserAccount user = authService.requireCurrentUser(authorization);
        ArchiveEntry entry = archiveEntryRepository.findByIdAndUserId(archiveId, user.getId())
                .orElseThrow(() -> new ArchiveNotFoundException(archiveId));

        PipelineResultResponse pipelineResult = null;
        VisualizationInfoResponse visualization = null;
        List<SearchPaperResponse> searchResults = List.of();
        List<ReaderPaperResponse> readerResults = List.of();
        List<RelevancePaperResponse> relevanceResults = List.of();

        if (entry.getRunId() != null && !entry.getRunId().isBlank()) {
            visualization = safeReadVisualization(entry.getTopic());
            pipelineResult = buildPipelineResult(entry, visualization);
            searchResults = safeReadSearchResults(entry.getRunId());
            readerResults = safeReadReaderResults(entry.getRunId());
            relevanceResults = safeReadRelevanceResults(entry.getRunId());
        }

        return ArchiveDetailResponse.from(
                entry,
                pipelineResult,
                visualization,
                searchResults,
                readerResults,
                relevanceResults
        );
    }

    @Transactional
    public void delete(String authorization, UUID archiveId) {
        UserAccount user = authService.requireCurrentUser(authorization);
        ArchiveEntry entry = archiveEntryRepository.findByIdAndUserId(archiveId, user.getId())
                .orElseThrow(() -> new ArchiveNotFoundException(archiveId));
        archiveEntryRepository.delete(entry);
    }

    @Transactional
    public void autoSavePipelineSnapshot(UserAccount user, PipelineRunMetadata metadata) {
        String title = metadata.topic();
        ReportResponse report = buildPipelineSnapshot(metadata);

        ArchiveEntry entry = archiveEntryRepository.findByUserIdAndReportId(user.getId(), metadata.reportId())
                .map(existing -> {
                    existing.updateFrom(metadata.runId(), title, report);
                    return existing;
                })
                .orElseGet(() -> new ArchiveEntry(user, metadata.reportId(), metadata.runId(), title, report));

        archiveEntryRepository.save(entry);
    }

    @Transactional
    public void autoSavePipelineSnapshot(UUID userId, UUID reportId) {
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        PipelineRunMetadata metadata = findPipelineMetadata(reportId).orElse(null);
        if (user == null || metadata == null) {
            return;
        }
        autoSavePipelineSnapshot(user, metadata);
    }

    private ReportResponse resolveReport(UUID reportId) {
        try {
            return reportService.get(reportId);
        } catch (ReportNotFoundException ignored) {
            return findPipelineMetadata(reportId)
                    .map(this::buildPipelineSnapshot)
                    .orElseThrow(() -> ignored);
        }
    }

    private String resolveRunId(UUID reportId) {
        try {
            PipelineRunMetadata metadata = findPipelineMetadata(reportId)
                    .orElseThrow(() -> new PipelineRunNotFoundException(reportId.toString()));
            return metadata.runId();
        } catch (PipelineRunNotFoundException ignored) {
            return null;
        }
    }

    private java.util.Optional<PipelineRunMetadata> findPipelineMetadata(UUID reportId) {
        return pipelineRunRegistry.findByReportId(reportId);
    }

    private ReportResponse buildPipelineSnapshot(PipelineRunMetadata metadata) {
        StatusSnapshot status = safeReadStatus(metadata.runId());
        AiReportResponse writerOutput = safeReadWriterOutput(metadata.runId());
        var createdAt = status.startedAt() == null ? metadata.createdAt() : status.startedAt();
        var updatedAt = status.finishedAt() == null ? createdAt : status.finishedAt();

        return new ReportResponse(
                metadata.reportId(),
                metadata.topic(),
                status.status(),
                writerOutput.gptDraft(),
                writerOutput.claudeDraft(),
                writerOutput.commonHighlights(),
                writerOutput.differentHighlights(),
                writerOutput.reviewResult(),
                writerOutput.mergedReport(),
                status.status() == ReportStatus.FAILED ? status.message() : null,
                createdAt,
                updatedAt
        );
    }

    private StatusSnapshot safeReadStatus(String runId) {
        try {
            return pipelineFileService.readStatus(runId);
        } catch (PipelineRunNotFoundException e) {
            return StatusSnapshot.pending();
        }
    }

    private AiReportResponse safeReadWriterOutput(String runId) {
        try {
            return pipelineFileService.readWriterOutput(runId);
        } catch (PipelineRunNotFoundException e) {
            return new AiReportResponse("", "", List.of(), List.of(), "", "");
        }
    }

    private PipelineResultResponse buildPipelineResult(ArchiveEntry entry, VisualizationInfoResponse visualization) {
        StatusSnapshot status = safeReadStatus(entry.getRunId());
        return new PipelineResultResponse(
                entry.getRunId(),
                entry.getReportId(),
                entry.getTopic(),
                status.currentStage().value(),
                status.searchCount(),
                status.summaryCount(),
                status.relevanceCount(),
                pipelineFileService.resolveReportPath(entry.getRunId()),
                visualization,
                status.startedAt(),
                status.finishedAt(),
                status.status(),
                status.message(),
                status.errorCode()
        );
    }

    private VisualizationInfoResponse safeReadVisualization(String topic) {
        try {
            return pipelineFileService.readVisualization(topic);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private List<SearchPaperResponse> safeReadSearchResults(String runId) {
        try {
            return pipelineFileService.readSearchResults(runId);
        } catch (PipelineRunNotFoundException e) {
            return List.of();
        }
    }

    private List<ReaderPaperResponse> safeReadReaderResults(String runId) {
        try {
            return pipelineFileService.readReaderResults(runId);
        } catch (PipelineRunNotFoundException e) {
            return List.of();
        }
    }

    private List<RelevancePaperResponse> safeReadRelevanceResults(String runId) {
        try {
            return pipelineFileService.readRelevanceResults(runId);
        } catch (PipelineRunNotFoundException e) {
            return List.of();
        }
    }
}
