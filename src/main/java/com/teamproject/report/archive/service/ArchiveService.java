package com.teamproject.report.archive.service;

import com.teamproject.report.archive.dto.ArchiveResponse;
import com.teamproject.report.archive.dto.SaveArchiveRequest;
import com.teamproject.report.archive.exception.ArchiveNotFoundException;
import com.teamproject.report.archive.model.ArchiveEntry;
import com.teamproject.report.archive.repository.ArchiveEntryRepository;
import com.teamproject.report.auth.model.UserAccount;
import com.teamproject.report.auth.repository.UserAccountRepository;
import com.teamproject.report.auth.service.AuthService;
import com.teamproject.report.pipeline.exception.PipelineRunNotFoundException;
import com.teamproject.report.pipeline.model.PipelineRunMetadata;
import com.teamproject.report.pipeline.model.StatusSnapshot;
import com.teamproject.report.pipeline.service.PipelineFileService;
import com.teamproject.report.pipeline.service.PipelineRunRegistry;
import com.teamproject.report.report.dto.ReportResponse;
import com.teamproject.report.report.dto.AiReportResponse;
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
}
