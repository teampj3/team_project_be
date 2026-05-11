package com.teamproject.report.report.controller;

import com.teamproject.report.config.AiServiceProperties;
import com.teamproject.report.report.client.AiReportClient;
import com.teamproject.report.report.dto.AiConnectionResponse;
import com.teamproject.report.report.dto.AiReportRequest;
import com.teamproject.report.report.dto.AiReportResponse;
import com.teamproject.report.report.dto.CreateReportRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiIntegrationController {

    private final AiReportClient aiReportClient;
    private final AiServiceProperties properties;

    public AiIntegrationController(AiReportClient aiReportClient, AiServiceProperties properties) {
        this.aiReportClient = aiReportClient;
        this.properties = properties;
    }

    @GetMapping("/health")
    public AiConnectionResponse health() {
        return new AiConnectionResponse(
                properties.baseUrl(),
                properties.localStubEnabled(),
                aiReportClient.health().status()
        );
    }

    @PostMapping("/reports/generate")
    public AiReportResponse generate(@Valid @RequestBody CreateReportRequest request) {
        return aiReportClient.generate(new AiReportRequest(UUID.randomUUID(), request.topic()));
    }
}
