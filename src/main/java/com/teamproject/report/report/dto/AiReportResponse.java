package com.teamproject.report.report.dto;

import java.util.List;

public record AiReportResponse(
        String gptDraft,
        String claudeDraft,
        List<String> commonHighlights,
        List<String> differentHighlights,
        String reviewResult,
        String mergedReport
) {
    public AiReportResponse {
        commonHighlights = commonHighlights == null ? List.of() : List.copyOf(commonHighlights);
        differentHighlights = differentHighlights == null ? List.of() : List.copyOf(differentHighlights);
    }
}
