package com.teamproject.report.pipeline.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamproject.report.config.PipelineProperties;
import com.teamproject.report.pipeline.dto.RelevancePaperResponse;
import com.teamproject.report.pipeline.dto.ReaderPaperResponse;
import com.teamproject.report.pipeline.dto.SearchPaperResponse;
import com.teamproject.report.pipeline.dto.VisualizationInfoResponse;
import com.teamproject.report.pipeline.exception.PipelineRunNotFoundException;
import com.teamproject.report.pipeline.model.PipelineStage;
import com.teamproject.report.pipeline.model.StatusSnapshot;
import com.teamproject.report.report.dto.AiReportResponse;
import com.teamproject.report.report.model.ReportStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    public List<ReaderPaperResponse> readReaderResults(String runId) {
        List<SearchResultFile> searchRows = readList(runId, "search_results.json", new TypeReference<>() {});
        List<ReaderResultFile> readerRows = readList(runId, "reader_results.json", new TypeReference<>() {});

        Map<String, SearchResultFile> searchById = new HashMap<>();
        for (SearchResultFile row : searchRows) {
            searchById.put(row.id, row);
        }

        List<ReaderPaperResponse> results = new ArrayList<>();
        for (ReaderResultFile row : readerRows) {
            SearchResultFile paper = searchById.get(row.id);
            if (paper == null) {
                continue;
            }
            results.add(new ReaderPaperResponse(
                    paper.title,
                    paper.authors == null ? List.of() : paper.authors,
                    paper.year,
                    paper.source,
                    row.summary
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

    public String resolvePreferredReportPath(String runId, String topic) {
        VisualizationInfoResponse visualization = readVisualization(topic);
        if (visualization != null && visualization.visualizedReportPath() != null) {
            return visualization.visualizedReportPath();
        }
        return resolveReportPath(runId);
    }

    public String readPreferredReportContent(String runId, String topic) {
        VisualizationInfoResponse visualization = readVisualization(topic);
        if (visualization != null && visualization.visualizedReportPath() != null) {
            String visualizedReport = readOutputContent(visualization.visualizedReportPath());
            if (visualizedReport != null) {
                return normalizeMarkdownAssetPaths(visualizedReport);
            }
        }

        Path reportPath = resolveRunDir(runId).resolve("report.md");
        if (!Files.exists(reportPath)) {
            return null;
        }

        try {
            return normalizeMarkdownAssetPaths(Files.readString(reportPath));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read report markdown", e);
        }
    }

    public VisualizationInfoResponse readVisualization(String topic) {
        Path visualizationsDir = resolveOutputsRoot().resolve("visualizations");
        if (!Files.isDirectory(visualizationsDir)) {
            return null;
        }

        try (Stream<Path> stream = Files.list(visualizationsDir)) {
            Path manifestPath = stream
                    .filter(path -> path.getFileName().toString().endsWith("_visualization_manifest.json"))
                    .filter(path -> matchesTopic(path, topic))
                    .max(Comparator.comparing(this::lastModifiedTime))
                    .orElse(null);

            if (manifestPath == null) {
                return null;
            }

            VisualizationManifestFile manifest = objectMapper.readValue(manifestPath.toFile(), VisualizationManifestFile.class);
            Map<String, String> assets = new LinkedHashMap<>();
            if (manifest.visualAssets != null) {
                for (Map.Entry<String, String> entry : manifest.visualAssets.entrySet()) {
                    assets.put(entry.getKey(), normalizeOutputPath(entry.getValue()));
                }
            }

            return new VisualizationInfoResponse(
                    normalizeOutputPath(manifestPath),
                    assets,
                    normalizeOutputPath(manifest.visualizedReport)
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read visualization output", e);
        }
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

    private Path resolveOutputsRoot() {
        Path runsRoot = Path.of(properties.getRunsRoot());
        Path fileName = runsRoot.getFileName();
        if (fileName != null && "runs".equals(fileName.toString()) && runsRoot.getParent() != null) {
            return runsRoot.getParent();
        }
        return runsRoot;
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

    private boolean matchesTopic(Path manifestPath, String topic) {
        try {
            VisualizationManifestFile manifest = objectMapper.readValue(manifestPath.toFile(), VisualizationManifestFile.class);
            return manifest.topic != null && manifest.topic.equalsIgnoreCase(topic);
        } catch (IOException e) {
            return false;
        }
    }

    private FileTime lastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    private String normalizeOutputPath(Path path) {
        return normalizeOutputPath(path == null ? null : path.toString());
    }

    private String normalizeOutputPath(String pathValue) {
        if (pathValue == null || pathValue.isBlank()) {
            return null;
        }

        Path outputsRoot = resolveOutputsRoot();
        Path path = Path.of(pathValue);
        if (path.startsWith(outputsRoot)) {
            return "outputs/" + outputsRoot.relativize(path).toString().replace('\\', '/');
        }

        String normalized = pathValue.replace('\\', '/');
        int outputsIndex = normalized.indexOf("/outputs/");
        if (outputsIndex >= 0) {
            return normalized.substring(outputsIndex + 1);
        }
        if (normalized.startsWith("outputs/")) {
            return normalized;
        }
        return normalized;
    }

    private String readOutputContent(String relativeOutputPath) {
        if (relativeOutputPath == null || relativeOutputPath.isBlank()) {
            return null;
        }

        String normalized = normalizeOutputPath(relativeOutputPath);
        if (normalized == null || !normalized.startsWith("outputs/")) {
            return null;
        }

        Path path = resolveOutputsRoot().resolve(normalized.substring("outputs/".length()));
        if (!Files.exists(path)) {
            return null;
        }

        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read output file: " + normalized, e);
        }
    }

    private String normalizeMarkdownAssetPaths(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }

        Path outputsRoot = resolveOutputsRoot().toAbsolutePath().normalize();
        String normalized = markdown.replace('\\', '/');
        String outputsRootPrefix = outputsRoot.toString().replace('\\', '/');

        normalized = normalized.replace(outputsRootPrefix + "/", "/outputs/");
        normalized = normalized.replace(outputsRootPrefix, "/outputs");
        normalized = normalized.replace("/app/outputs/", "/outputs/");
        normalized = normalized.replace("/pipeline/outputs/", "/outputs/");
        normalized = normalized.replaceAll("/(?:[^\\s)\"']+/)*outputs/", "/outputs/");

        return normalized;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class VisualizationManifestFile {
        public String topic;
        @JsonProperty("visual_assets")
        public Map<String, String> visualAssets;
        @JsonProperty("visualized_report")
        public String visualizedReport;
    }
}
