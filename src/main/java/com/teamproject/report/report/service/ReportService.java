package com.teamproject.report.report.service;

import com.teamproject.report.report.client.AiReportClient;
import com.teamproject.report.pipeline.service.PipelineReportService;
import com.teamproject.report.report.dto.AiReportRequest;
import com.teamproject.report.report.dto.ReportResponse;
import com.teamproject.report.report.model.Report;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReportService {

    private final AiReportClient aiReportClient;
    private final Map<UUID, Report> reports = new ConcurrentHashMap<>();

    public ReportService(AiReportClient aiReportClient) {
        this.aiReportClient = aiReportClient;
    }

    public ReportResponse create(String topic) {
        Report report = Report.create(topic);
        reports.put(report.getId(), report);

        try {
            report.complete(aiReportClient.generate(new AiReportRequest(report.getId(), topic)));
        } catch (RuntimeException e) {
            report.fail(e.getMessage());
        }

        return ReportResponse.from(report);
    }

    public ReportResponse get(UUID reportId) {
        Report report = reports.get(reportId);
        if (report == null) {
            throw new ReportNotFoundException(reportId);
        }
        return ReportResponse.from(report);
    }

    public ReportResponse getOrPipeline(UUID reportId, PipelineReportService pipelineReportService) {
        Report report = reports.get(reportId);
        if (report != null) {
            return ReportResponse.from(report);
        }
        return pipelineReportService.getByReportId(reportId);
    }

    public List<ReportResponse> list() {
        return reports.values().stream()
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .map(ReportResponse::from)
                .toList();
    }
}
