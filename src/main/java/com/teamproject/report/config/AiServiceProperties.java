package com.teamproject.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-service")
public record AiServiceProperties(
        String baseUrl,
        String generatePath,
        int timeoutSeconds,
        boolean localStubEnabled
) {
}
