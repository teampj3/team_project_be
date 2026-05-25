package com.teamproject.report.pipeline.service;

import com.teamproject.report.config.PipelineProperties;
import com.teamproject.report.archive.service.ArchiveService;
import com.teamproject.report.auth.model.UserAccount;
import com.teamproject.report.auth.service.AuthService;
import com.teamproject.report.pipeline.dto.PipelineRunRequest;
import com.teamproject.report.pipeline.dto.PipelineResultResponse;
import com.teamproject.report.pipeline.dto.PipelineRunResponse;
import com.teamproject.report.pipeline.dto.RelevancePaperResponse;
import com.teamproject.report.pipeline.dto.SearchPaperResponse;
import com.teamproject.report.pipeline.dto.VisualizationInfoResponse;
import com.teamproject.report.pipeline.exception.PipelineRunNotFoundException;
import com.teamproject.report.pipeline.exception.PipelineStartException;
import com.teamproject.report.pipeline.model.PipelineRunMetadata;
import com.teamproject.report.pipeline.model.StatusSnapshot;
import com.teamproject.report.report.dto.AiReportResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class PipelineService {

    private final PipelineProperties properties;
    private final PipelineRunRegistry registry;
    private final PipelineFileService pipelineFileService;
    private final WebClient aiWebClient;
    private final AuthService authService;
    private final ArchiveService archiveService;

    public PipelineService(
            PipelineProperties properties,
            PipelineRunRegistry registry,
            PipelineFileService pipelineFileService,
            WebClient aiWebClient,
            AuthService authService,
            ArchiveService archiveService
    ) {
        this.properties = properties;
        this.registry = registry;
        this.pipelineFileService = pipelineFileService;
        this.aiWebClient = aiWebClient;
        this.authService = authService;
        this.archiveService = archiveService;
    }

    public PipelineRunResponse startRun(String authorization, String topic) {
        try {
            UserAccount user = resolveCurrentUser(authorization);
            PipelineRunResponse pythonResponse = aiWebClient.post()
                    .uri(properties.getRunPath())
                    .bodyValue(new PipelineRunRequest(topic))
                    .retrieve()
                    .bodyToMono(PipelineRunResponse.class)
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            if (pythonResponse == null || pythonResponse.runId() == null || pythonResponse.runId().isBlank()) {
                throw new PipelineStartException("Python pipeline did not return runId");
            }

            String runId = pythonResponse.runId();
            PipelineRunMetadata metadata = new PipelineRunMetadata(
                    runId,
                    UUID.randomUUID(),
                    user == null ? null : user.getId(),
                    topic,
                    Instant.now()
            );
            registry.register(metadata);
            if (user != null) {
                archiveService.autoSavePipelineSnapshot(user, metadata);
            }

            StatusSnapshot status = safeReadStatus(runId);
            return new PipelineRunResponse(
                    runId,
                    topic,
                    status.status(),
                    status.currentStage().value(),
                    status.message(),
                    status.errorCode()
            );
        } catch (Exception e) {
            throw new PipelineStartException("Failed to start Python pipeline", e);
        }
    }

    public PipelineResultResponse getResult(String runId) {
        PipelineRunMetadata metadata = registry.findByRunId(runId)
                .orElseThrow(() -> new PipelineRunNotFoundException(runId));

        StatusSnapshot status = safeReadStatus(runId);
        VisualizationInfoResponse visualization = pipelineFileService.readVisualization(metadata.topic());
        if (metadata.userId() != null) {
            archiveService.autoSavePipelineSnapshot(metadata.userId(), metadata.reportId());
        }
        return new PipelineResultResponse(
                runId,
                metadata.reportId(),
                metadata.topic(),
                status.currentStage().value(),
                status.searchCount(),
                status.summaryCount(),
                status.relevanceCount(),
                pipelineFileService.resolvePreferredReportPath(runId, metadata.topic()),
                visualization,
                status.startedAt(),
                status.finishedAt(),
                status.status(),
                status.message(),
                status.errorCode()
        );
    }

    public List<SearchPaperResponse> getSearchResults(String runId) {
        ensureKnownRun(runId);
        return pipelineFileService.readSearchResults(runId);
    }

    public List<RelevancePaperResponse> getRelevanceResults(String runId) {
        ensureKnownRun(runId);
        return pipelineFileService.readRelevanceResults(runId);
    }

    public AiReportResponse getWriterOutput(String runId) {
        ensureKnownRun(runId);
        return pipelineFileService.readWriterOutput(runId);
    }

    public PipelineRunMetadata getLatestRun() {
        return registry.findLatest().orElseThrow(() -> new PipelineRunNotFoundException("latest"));
    }

    public PipelineRunMetadata getRunByReportId(UUID reportId) {
        return registry.findByReportId(reportId)
                .orElseThrow(() -> new PipelineRunNotFoundException(reportId.toString()));
    }

    private void ensureKnownRun(String runId) {
        if (registry.findByRunId(runId).isEmpty()) {
            throw new PipelineRunNotFoundException(runId);
        }
    }

    private StatusSnapshot safeReadStatus(String runId) {
        try {
            return pipelineFileService.readStatus(runId);
        } catch (PipelineRunNotFoundException e) {
            return StatusSnapshot.pending();
        }
    }

    private UserAccount resolveCurrentUser(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        return authService.requireCurrentUser(authorization);
    }
}
