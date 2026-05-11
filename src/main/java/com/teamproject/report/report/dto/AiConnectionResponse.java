package com.teamproject.report.report.dto;

public record AiConnectionResponse(
        String baseUrl,
        boolean stubEnabled,
        String status
) {
}
