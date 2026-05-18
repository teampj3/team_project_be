package com.teamproject.report.archive.service;

import com.teamproject.report.archive.dto.ArchiveResponse;
import com.teamproject.report.archive.dto.SaveArchiveRequest;
import com.teamproject.report.archive.exception.ArchiveNotFoundException;
import com.teamproject.report.archive.model.ArchiveEntry;
import com.teamproject.report.archive.repository.ArchiveEntryRepository;
import com.teamproject.report.auth.model.UserAccount;
import com.teamproject.report.auth.service.AuthService;
import com.teamproject.report.pipeline.exception.PipelineRunNotFoundException;
import com.teamproject.report.pipeline.model.PipelineRunMetadata;
import com.teamproject.report.pipeline.service.PipelineService;
import com.teamproject.report.pipeline.service.PipelineReportService;
import com.teamproject.report.report.dto.ReportResponse;
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
    private final ReportService reportService;
    private final PipelineReportService pipelineReportService;
    private final PipelineService pipelineService;

    public ArchiveService(
            ArchiveEntryRepository archiveEntryRepository,
            AuthService authService,
            ReportService reportService,
            PipelineReportService pipelineReportService,
            PipelineService pipelineService
    ) {
        this.archiveEntryRepository = archiveEntryRepository;
        this.authService = authService;
        this.reportService = reportService;
        this.pipelineReportService = pipelineReportService;
        this.pipelineService = pipelineService;
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

    private ReportResponse resolveReport(UUID reportId) {
        try {
            return reportService.get(reportId);
        } catch (ReportNotFoundException ignored) {
            return pipelineReportService.getByReportId(reportId);
        }
    }

    private String resolveRunId(UUID reportId) {
        try {
            PipelineRunMetadata metadata = pipelineService.getRunByReportId(reportId);
            return metadata.runId();
        } catch (PipelineRunNotFoundException ignored) {
            return null;
        }
    }
}
