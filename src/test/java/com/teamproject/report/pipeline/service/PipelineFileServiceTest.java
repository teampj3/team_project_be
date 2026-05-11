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
        Path runDir = tempDir.resolve("run-123");
        Files.createDirectories(runDir);

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

        PipelineProperties properties = new PipelineProperties();
        properties.setRunsRoot(tempDir.toString());

        PipelineFileService service = new PipelineFileService(new ObjectMapper(), properties);

        assertThat(service.readStatus("run-123").status()).isEqualTo(ReportStatus.COMPLETED);

        List<SearchPaperResponse> searchResults = service.readSearchResults("run-123");
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.getFirst().summary()).isEqualTo("Reader summary");

        List<RelevancePaperResponse> relevanceResults = service.readRelevanceResults("run-123");
        assertThat(relevanceResults).hasSize(1);
        assertThat(relevanceResults.getFirst().relevanceScore()).isEqualTo(91.2);
        assertThat(relevanceResults.getFirst().selected()).isTrue();
    }
}
