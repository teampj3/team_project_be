package com.teamproject.report.archive.service;

import com.teamproject.report.archive.dto.ArchiveResponse;
import com.teamproject.report.archive.dto.SaveArchiveRequest;
import com.teamproject.report.auth.dto.AuthResponse;
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
}
