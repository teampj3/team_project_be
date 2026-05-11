package com.teamproject.report.report.controller;

import com.teamproject.report.report.dto.CreateReportRequest;
import com.teamproject.report.report.dto.ReportResponse;
import com.teamproject.report.pipeline.service.PipelineReportService;
import com.teamproject.report.report.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final PipelineReportService pipelineReportService;

    public ReportController(ReportService reportService, PipelineReportService pipelineReportService) {
        this.reportService = reportService;
        this.pipelineReportService = pipelineReportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@Valid @RequestBody CreateReportRequest request) {
        return reportService.create(request.topic());
    }

    @GetMapping("/latest")
    public ReportResponse latest() {
        return pipelineReportService.getLatest();
    }

    @GetMapping("/{reportId}")
    public ReportResponse get(@PathVariable UUID reportId) {
        return reportService.getOrPipeline(reportId, pipelineReportService);
    }

    @GetMapping
    public List<ReportResponse> list() {
        return reportService.list();
    }
}
