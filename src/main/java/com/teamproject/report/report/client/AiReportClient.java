package com.teamproject.report.report.client;

import com.teamproject.report.config.AiServiceProperties;
import com.teamproject.report.report.dto.AiReportRequest;
import com.teamproject.report.report.dto.AiReportResponse;
import com.teamproject.report.report.dto.AiServiceHealthResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Component
public class AiReportClient {

    private final WebClient aiWebClient;
    private final AiServiceProperties properties;

    public AiReportClient(WebClient aiWebClient, AiServiceProperties properties) {
        this.aiWebClient = aiWebClient;
        this.properties = properties;
    }

    public AiReportResponse generate(AiReportRequest request) {
        if (properties.localStubEnabled()) {
            return stubResponse(request.topic());
        }

        return aiWebClient.post()
                .uri(properties.generatePath())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiReportResponse.class)
                .block(Duration.ofSeconds(properties.timeoutSeconds()));
    }

    public AiServiceHealthResponse health() {
        if (properties.localStubEnabled()) {
            return new AiServiceHealthResponse("stub");
        }

        return aiWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(AiServiceHealthResponse.class)
                .block(Duration.ofSeconds(properties.timeoutSeconds()));
    }

    private AiReportResponse stubResponse(String topic) {
        return new AiReportResponse(
                "GPT draft for topic: " + topic,
                "Claude draft for topic: " + topic,
                List.of("Shared core argument for " + topic),
                List.of("Different emphasis between GPT and Claude"),
                "Review result placeholder. Connect Python AI service to replace this.",
                "Merged final report placeholder for " + topic
        );
    }
}
