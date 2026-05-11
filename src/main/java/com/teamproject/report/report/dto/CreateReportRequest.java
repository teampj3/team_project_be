package com.teamproject.report.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotBlank(message = "topic is required")
        @Size(max = 200, message = "topic must be 200 characters or less")
        String topic
) {
}
