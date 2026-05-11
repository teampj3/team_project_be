package com.teamproject.report.pipeline.dto;

import java.util.List;

public record SearchPaperResponse(
        String title,
        List<String> authors,
        String year,
        String source,
        String summary
) {
}
