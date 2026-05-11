package com.teamproject.report.report.dto;

import java.util.UUID;

public record AiReportRequest(
        UUID reportId,
        String topic
) {
}
