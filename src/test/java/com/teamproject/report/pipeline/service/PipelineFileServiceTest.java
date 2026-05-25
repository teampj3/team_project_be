package com.teamproject.report.pipeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamproject.report.config.PipelineProperties;
import com.teamproject.report.pipeline.dto.RelevancePaperResponse;
import com.teamproject.report.pipeline.dto.SearchPaperResponse;
import com.teamproject.report.report.model.ReportStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineFileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void readsRunScopedFilesAndBuildsFrontendDtos() throws Exception {
        Path outputsDir = tempDir.resolve("outputs");
        Path runsDir = outputsDir.resolve("runs");
        Path runDir = runsDir.resolve("run-123");
        Files.createDirectories(runDir);
        Files.createDirectories(outputsDir.resolve("visualizations"));
        Files.createDirectories(outputsDir.resolve("reports"));

        Files.writeString(runDir.resolve("status.json"), """
                {
                  "current_stage": "writer",
                  "search_count": 3,
                  "summary_count": 2,
                  "relevance_count": 1,
                  "started_at": "2026-05-11T10:00:00Z",
                  "finished_at": "2026-05-11T10:05:00Z",
                  "failed_stage": null,
                  "error_message": null
                }
                """);
        Files.writeString(runDir.resolve("search_results.json"), """
                [
                  {
                    "id": "p1",
                    "title": "Paper 1",
                    "authors": ["A1", "A2"],
                    "year": "2025",
                    "source": "Semantic Scholar",
                    "abstract": "Abstract",
                    "snippet": "Snippet"
                  }
                ]
                """);
        Files.writeString(runDir.resolve("reader_results.json"), """
                [
                  {
                    "id": "p1",
                    "summary": "Reader summary"
                  }
                ]
                """);
        Files.writeString(runDir.resolve("relevance_results.json"), """
                [
                  {
                    "id": "p1",
                    "relevance_score": 91.2,
                    "selected": true,
                    "reason": "High match"
                  }
                ]
                """);
        Files.writeString(outputsDir.resolve("visualizations").resolve("code_review_visualization_manifest.json"), """
                {
                  "topic": "code review",
                  "visual_assets": {
                    "visual_1": "/tmp/outputs/visualizations/code_review_visual_1.png"
                  },
                  "visualized_report": "/tmp/outputs/reports/code_review_visualized.md"
                }
                """);
        Files.writeString(outputsDir.resolve("reports").resolve("code_review_visualized.md"), """
                # Visualized Report

                ![visual](/tmp/outputs/visualizations/code_review_visual_1.png)
                """);

        PipelineProperties properties = new PipelineProperties();
        properties.setRunsRoot(runsDir.toString());

        PipelineFileService service = new PipelineFileService(new ObjectMapper(), properties);

        assertThat(service.readStatus("run-123").status()).isEqualTo(ReportStatus.COMPLETED);

        List<SearchPaperResponse> searchResults = service.readSearchResults("run-123");
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.getFirst().summary()).isEqualTo("Reader summary");

        List<RelevancePaperResponse> relevanceResults = service.readRelevanceResults("run-123");
        assertThat(relevanceResults).hasSize(1);
        assertThat(relevanceResults.getFirst().relevanceScore()).isEqualTo(91.2);
        assertThat(relevanceResults.getFirst().selected()).isTrue();

        assertThat(service.readVisualization("code review")).isNotNull();
        assertThat(service.readVisualization("code review").manifestPath())
                .isEqualTo("outputs/visualizations/code_review_visualization_manifest.json");
        assertThat(service.readVisualization("code review").assets())
                .containsEntry("visual_1", "outputs/visualizations/code_review_visual_1.png");
        assertThat(service.readVisualization("code review").visualizedReportPath())
                .isEqualTo("outputs/reports/code_review_visualized.md");
        assertThat(service.resolvePreferredReportPath("run-123", "code review"))
                .isEqualTo("outputs/reports/code_review_visualized.md");
        assertThat(service.readPreferredReportContent("run-123", "code review"))
                .contains("![visual](/outputs/visualizations/code_review_visual_1.png)");
    }
}
