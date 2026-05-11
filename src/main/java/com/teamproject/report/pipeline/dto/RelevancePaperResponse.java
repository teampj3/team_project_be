package com.teamproject.report.pipeline.dto;

import java.util.List;

public record RelevancePaperResponse(
        String title,
        List<String> authors,
        String year,
        String source,
        String summary,
        double relevanceScore,
        boolean selected
) {
}
