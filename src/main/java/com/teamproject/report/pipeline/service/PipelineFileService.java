package com.teamproject.report.pipeline.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamproject.report.config.PipelineProperties;
import com.teamproject.report.pipeline.dto.RelevancePaperResponse;
import com.teamproject.report.pipeline.dto.SearchPaperResponse;
import com.teamproject.report.pipeline.exception.PipelineRunNotFoundException;
import com.teamproject.report.pipeline.model.PipelineStage;
import com.teamproject.report.pipeline.model.StatusSnapshot;
import com.teamproject.report.report.dto.AiReportResponse;
import com.teamproject.report.report.model.ReportStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PipelineFileService {

    private final ObjectMapper objectMapper;
    private final PipelineProperties properties;

    public PipelineFileService(ObjectMapper objectMapper, PipelineProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public StatusSnapshot readStatus(String runId) {
        Path statusPath = resolveRunDir(runId).resolve("status.json");
        if (!Files.exists(statusPath)) {
            return StatusSnapshot.pending();
        }

        try {
            StatusFile statusFile = objectMapper.readValue(statusPath.toFile(), StatusFile.class);
            PipelineStage currentStage = PipelineStage.fromStatus(statusFile.currentStage, statusFile.failedStage);
            ReportStatus status = mapStatus(statusFile);
            String message = firstNonBlank(
                    statusFile.message,
                    isLegacySearchEmptyResult(statusFile) ? "Search completed with 0 results." : statusFile.errorMessage
            );
            String errorCode = firstNonBlank(
                    statusFile.errorCode,
                    isLegacySearchEmptyResult(statusFile)
                            ? "SEARCH_EMPTY_RESULT"
                            : (status == ReportStatus.FAILED ? "PIPELINE_FAILED" : null)
            );
            return new StatusSnapshot(
                    statusFile.currentStage,
                    currentStage,
                    statusFile.searchCount,
                    statusFile.summaryCount,
                    statusFile.relevanceCount,
                    parseInstant(statusFile.startedAt),
                    parseInstant(statusFile.finishedAt),
                    status,
                    statusFile.failedStage,
                    message,
                    errorCode
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read pipeline status file", e);
        }
    }

    public List<SearchPaperResponse> readSearchResults(String runId) {
        List<SearchResultFile> searchRows = readList(runId, "search_results.json", new TypeReference<>() {});
        List<ReaderResultFile> readerRows = readList(runId, "reader_results.json", new TypeReference<>() {});
        Map<String, String> summaryById = new HashMap<>();
        for (ReaderResultFile row : readerRows) {
            summaryById.put(row.id, row.summary);
        }

        List<SearchPaperResponse> results = new ArrayList<>();
        for (SearchResultFile row : searchRows) {
            results.add(new SearchPaperResponse(
                    row.title,
                    row.authors == null ? List.of() : row.authors,
                    row.year,
                    row.source,
                    summaryById.getOrDefault(row.id, "")
            ));
        }
        return results;
    }

    public List<RelevancePaperResponse> readRelevanceResults(String runId) {
        List<SearchResultFile> searchRows = readList(runId, "search_results.json", new TypeReference<>() {});
        List<ReaderResultFile> readerRows = readList(runId, "reader_results.json", new TypeReference<>() {});
        List<RelevanceResultFile> relevanceRows = readList(runId, "relevance_results.json", new TypeReference<>() {});

        Map<String, SearchResultFile> searchById = new HashMap<>();
        for (SearchResultFile row : searchRows) {
            searchById.put(row.id, row);
        }

        Map<String, String> summaryById = new HashMap<>();
        for (ReaderResultFile row : readerRows) {
            summaryById.put(row.id, row.summary);
        }

        List<RelevancePaperResponse> results = new ArrayList<>();
        for (RelevanceResultFile row : relevanceRows) {
            SearchResultFile paper = searchById.get(row.id);
            if (paper == null) {
                continue;
            }
            results.add(new RelevancePaperResponse(
                    paper.title,
                    paper.authors == null ? List.of() : paper.authors,
                    paper.year,
                    paper.source,
                    summaryById.getOrDefault(row.id, ""),
                    row.relevanceScore,
                    row.selected
            ));
        }
        return results;
    }

    public AiReportResponse readWriterOutput(String runId) {
        Path writerPath = resolveRunDir(runId).resolve("writer_output.json");
        if (!Files.exists(writerPath)) {
            return new AiReportResponse("", "", List.of(), List.of(), "", "");
        }

        try {
            return objectMapper.readValue(writerPath.toFile(), AiReportResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read writer output file", e);
        }
    }

    public String resolveReportPath(String runId) {
        Path reportPath = resolveRunDir(runId).resolve("report.md");
        return Files.exists(reportPath) ? "outputs/runs/" + runId + "/report.md" : null;
    }

    private ReportStatus mapStatus(StatusFile statusFile) {
        if (statusFile.status != null && !statusFile.status.isBlank()) {
            return ReportStatus.valueOf(statusFile.status.trim().toUpperCase());
        }
        if (isLegacySearchEmptyResult(statusFile)) {
            return ReportStatus.COMPLETED;
        }
        if (statusFile.errorMessage != null && !statusFile.errorMessage.isBlank()) {
            return ReportStatus.FAILED;
        }
        if (statusFile.finishedAt != null && !statusFile.finishedAt.isBlank()) {
            return ReportStatus.COMPLETED;
        }
        if (statusFile.currentStage == null || statusFile.currentStage.isBlank()) {
            return ReportStatus.PENDING;
        }
        if ("failed".equalsIgnoreCase(statusFile.currentStage)) {
            return ReportStatus.FAILED;
        }
        if ("completed".equalsIgnoreCase(statusFile.currentStage)) {
            return ReportStatus.COMPLETED;
        }
        return ReportStatus.PROCESSING;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private boolean isLegacySearchEmptyResult(StatusFile statusFile) {
        return "search".equalsIgnoreCase(statusFile.failedStage)
                && statusFile.searchCount == 0
                && "검색 결과 없음".equals(statusFile.errorMessage);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }

    private Path resolveRunDir(String runId) {
        Path runDir = Path.of(properties.getRunsRoot(), runId);
        if (!Files.exists(runDir)) {
            throw new PipelineRunNotFoundException(runId);
        }
        return runDir;
    }

    private <T> List<T> readList(String runId, String fileName, TypeReference<List<T>> typeReference) {
        Path path = resolveRunDir(runId).resolve(fileName);
        if (!Files.exists(path)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(path.toFile(), typeReference);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read pipeline file: " + fileName, e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StatusFile {
        public String status;
        @JsonProperty("current_stage")
        public String currentStage;
        public String message;
        @JsonProperty("error_code")
        public String errorCode;
        @JsonProperty("search_count")
        public int searchCount;
        @JsonProperty("summary_count")
        public int summaryCount;
        @JsonProperty("relevance_count")
        public int relevanceCount;
        @JsonProperty("started_at")
        public String startedAt;
        @JsonProperty("finished_at")
        public String finishedAt;
        @JsonProperty("failed_stage")
        public String failedStage;
        @JsonProperty("error_message")
        public String errorMessage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SearchResultFile {
        public String id;
        public String title;
        public List<String> authors;
        public String year;
        public String source;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ReaderResultFile {
        public String id;
        public String summary;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RelevanceResultFile {
        public String id;
        @JsonProperty("relevance_score")
        public double relevanceScore;
        public boolean selected;
    }
}
