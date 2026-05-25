package com.teamproject.report.archive.service;

import com.teamproject.report.archive.dto.ArchiveDetailResponse;
import com.teamproject.report.archive.dto.ArchiveResponse;
import com.teamproject.report.archive.dto.SaveArchiveRequest;
import com.teamproject.report.auth.dto.AuthResponse;
import com.teamproject.report.config.PipelineProperties;
import com.teamproject.report.auth.service.AuthService;
import com.teamproject.report.pipeline.model.PipelineRunMetadata;
import com.teamproject.report.pipeline.service.PipelineRunRegistry;
import com.teamproject.report.report.dto.ReportResponse;
import com.teamproject.report.report.model.ReportStatus;
import com.teamproject.report.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ArchiveServiceTest {

    @Autowired
    private ArchiveService archiveService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private PipelineRunRegistry pipelineRunRegistry;

    @Autowired
    private PipelineProperties pipelineProperties;

    @Test
    void saveAndReadArchiveSnapshot() {
        AuthResponse auth = authService.signUp("archive@example.com", "Archiver", "password123");
        ReportResponse report = reportService.create("code review");

        ArchiveResponse saved = archiveService.save(
                "Bearer " + auth.accessToken(),
                new SaveArchiveRequest(report.id(), "첫 번째 아카이브")
        );

        assertThat(saved.reportId()).isEqualTo(report.id());
        assertThat(saved.title()).isEqualTo("첫 번째 아카이브");
        assertThat(saved.topic()).isEqualTo("code review");
        assertThat(saved.archiveId()).isNotNull();

        List<ArchiveResponse> archives = archiveService.list("Bearer " + auth.accessToken());
        assertThat(archives).hasSize(1);
        assertThat(archives.getFirst().archiveId()).isEqualTo(saved.archiveId());

        ArchiveResponse detail = archiveService.get("Bearer " + auth.accessToken(), saved.archiveId());
        assertThat(detail.mergedReport()).isEqualTo(report.mergedReport());
    }

    @Test
    void autoSavePipelineSnapshotCreatesPlaceholderArchive() {
        AuthResponse auth = authService.signUp("pipeline-archive@example.com", "Piper", "password123");
        UUID reportId = UUID.randomUUID();
        pipelineRunRegistry.register(new PipelineRunMetadata(
                "run-placeholder",
                reportId,
                auth.userId(),
                "code review",
                Instant.parse("2026-05-18T00:00:00Z")
        ));

        archiveService.autoSavePipelineSnapshot(auth.userId(), reportId);

        List<ArchiveResponse> archives = archiveService.list("Bearer " + auth.accessToken());
        assertThat(archives).hasSize(1);
        assertThat(archives.getFirst().reportId()).isEqualTo(reportId);
        assertThat(archives.getFirst().topic()).isEqualTo("code review");
        assertThat(archives.getFirst().status()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    void archiveDetailRestoresWorkspaceData() throws Exception {
        AuthResponse auth = authService.signUp("workspace-archive@example.com", "Workspace", "password123");
        String runId = "run-workspace-detail";
        UUID reportId = UUID.randomUUID();
        pipelineRunRegistry.register(new PipelineRunMetadata(
                runId,
                reportId,
                auth.userId(),
                "code review",
                Instant.parse("2026-05-18T00:00:00Z")
        ));

        Path runDir = Path.of(pipelineProperties.getRunsRoot(), runId);
        Files.createDirectories(runDir);
        Path outputsRoot = Path.of(pipelineProperties.getRunsRoot()).getParent();
        Files.createDirectories(outputsRoot.resolve("visualizations"));
        Files.createDirectories(outputsRoot.resolve("reports"));
        Files.writeString(runDir.resolve("status.json"), """
                {
                  "status": "COMPLETED",
                  "current_stage": "writer",
                  "message": "Pipeline completed successfully.",
                  "error_code": null,
                  "search_count": 1,
                  "summary_count": 1,
                  "relevance_count": 1,
                  "started_at": "2026-05-18T00:00:00Z",
                  "finished_at": "2026-05-18T00:10:00Z",
                  "failed_stage": null,
                  "error_message": null
                }
                """);
        Files.writeString(runDir.resolve("search_results.json"), """
                [
                  {
                    "id": "paper-1",
                    "title": "Code Review with LLMs",
                    "authors": ["Jane Doe"],
                    "year": "2026",
                    "source": "Semantic Scholar",
                    "abstract": "",
                    "snippet": ""
                  }
                ]
                """);
        Files.writeString(runDir.resolve("reader_results.json"), """
                [
                  {
                    "id": "paper-1",
                    "summary": "Reader summary"
                  }
                ]
                """);
        Files.writeString(runDir.resolve("relevance_results.json"), """
                [
                  {
                    "id": "paper-1",
                    "relevance_score": 0.91,
                    "selected": true,
                    "reason": "Relevant"
                  }
                ]
                """);
        Files.writeString(runDir.resolve("writer_output.json"), """
                {
                  "gptDraft": "",
                  "claudeDraft": "Partial draft",
                  "commonHighlights": ["Highlight A"],
                  "differentHighlights": [],
                  "reviewResult": "",
                  "mergedReport": "Merged draft"
                }
                """);
        Files.writeString(runDir.resolve("report.md"), "# Report");
        Files.writeString(outputsRoot.resolve("visualizations").resolve("code_review_visualization_manifest.json"), """
                {
                  "topic": "code review",
                  "visual_assets": {
                    "visual_1": "/app/outputs/visualizations/code_review_visual_1.png",
                    "visual_2": "/app/outputs/visualizations/code_review_visual_2.png"
                  },
                  "visualized_report": "/app/outputs/reports/code_review_visualized.md"
                }
                """);
        Files.writeString(outputsRoot.resolve("reports").resolve("code_review_visualized.md"), """
                # Visualized Report

                ![visual_1](/app/outputs/visualizations/code_review_visual_1.png)
                """);

        archiveService.autoSavePipelineSnapshot(auth.userId(), reportId);
        ArchiveResponse saved = archiveService.list("Bearer " + auth.accessToken()).getFirst();

        ArchiveDetailResponse detail = archiveService.getDetail("Bearer " + auth.accessToken(), saved.archiveId());
        assertThat(detail.pipelineResult()).isNotNull();
        assertThat(detail.pipelineResult().currentStage()).isEqualTo("writer");
        assertThat(detail.searchResults()).hasSize(1);
        assertThat(detail.readerResults()).hasSize(1);
        assertThat(detail.relevanceResults()).hasSize(1);
        assertThat(detail.searchResults().getFirst().title()).isEqualTo("Code Review with LLMs");
        assertThat(detail.readerResults().getFirst().summary()).isEqualTo("Reader summary");
        assertThat(detail.relevanceResults().getFirst().relevanceScore()).isEqualTo(0.91);
        assertThat(detail.visualization()).isNotNull();
        assertThat(detail.visualization().manifestPath())
                .isEqualTo("outputs/visualizations/code_review_visualization_manifest.json");
        assertThat(detail.visualization().assets())
                .containsEntry("visual_1", "outputs/visualizations/code_review_visual_1.png")
                .containsEntry("visual_2", "outputs/visualizations/code_review_visual_2.png");
        assertThat(detail.visualization().visualizedReportPath())
                .isEqualTo("outputs/reports/code_review_visualized.md");
        assertThat(detail.pipelineResult().reportPath())
                .isEqualTo("outputs/reports/code_review_visualized.md");
        assertThat(detail.mergedReport())
                .contains("![visual_1](/outputs/visualizations/code_review_visual_1.png)");
    }

    @Test
    void deleteRemovesOwnedArchive() {
        AuthResponse auth = authService.signUp("archive-delete@example.com", "Delete Me", "password123");
        ReportResponse report = reportService.create("code review");

        ArchiveResponse saved = archiveService.save(
                "Bearer " + auth.accessToken(),
                new SaveArchiveRequest(report.id(), "삭제 테스트")
        );

        archiveService.delete("Bearer " + auth.accessToken(), saved.archiveId());

        List<ArchiveResponse> archives = archiveService.list("Bearer " + auth.accessToken());
        assertThat(archives).isEmpty();
    }
}
