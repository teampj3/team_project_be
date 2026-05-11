package com.teamproject.report.pipeline.service;

import com.teamproject.report.pipeline.model.PipelineRunMetadata;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PipelineRunRegistry {

    private final Map<String, PipelineRunMetadata> byRunId = new ConcurrentHashMap<>();
    private final Map<UUID, String> byReportId = new ConcurrentHashMap<>();

    public void register(PipelineRunMetadata metadata) {
        byRunId.put(metadata.runId(), metadata);
        byReportId.put(metadata.reportId(), metadata.runId());
    }

    public Optional<PipelineRunMetadata> findByRunId(String runId) {
        return Optional.ofNullable(byRunId.get(runId));
    }

    public Optional<PipelineRunMetadata> findByReportId(UUID reportId) {
        String runId = byReportId.get(reportId);
        return runId == null ? Optional.empty() : findByRunId(runId);
    }

    public Optional<PipelineRunMetadata> findLatest() {
        return byRunId.values().stream()
                .max(Comparator.comparing(PipelineRunMetadata::createdAt));
    }
}
