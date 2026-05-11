package com.teamproject.report.report.service;

import com.teamproject.report.report.client.AiReportClient;
import com.teamproject.report.report.dto.AiReportRequest;
import com.teamproject.report.report.dto.AiReportResponse;
import com.teamproject.report.report.dto.ReportResponse;
import com.teamproject.report.report.model.ReportStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceTest {

    @Test
    void createStoresCompletedReport() {
        AiReportClient client = new StubAiReportClient();
        ReportService service = new ReportService(client);

        ReportResponse response = service.create("AI report architecture");

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(response.gptDraft()).contains("GPT");
        assertThat(service.get(response.id()).mergedReport()).contains("Merged");
        assertThat(service.list()).hasSize(1);
    }

    private static class StubAiReportClient extends AiReportClient {

        StubAiReportClient() {
            super(null, null);
        }

        @Override
        public AiReportResponse generate(AiReportRequest request) {
            return new AiReportResponse(
                    "GPT draft",
                    "Claude draft",
                    List.of("common"),
                    List.of("different"),
                    "Review",
                    "Merged report"
            );
        }
    }
}
