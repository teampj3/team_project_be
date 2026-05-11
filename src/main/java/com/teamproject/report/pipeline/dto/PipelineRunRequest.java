package com.teamproject.report.pipeline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PipelineRunRequest(
        @NotBlank(message = "topic is required")
        @Size(max = 200, message = "topic must be 200 characters or less")
        String topic
) {
}
