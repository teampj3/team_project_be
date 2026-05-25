package com.teamproject.report.pipeline.dto;

import java.util.List;

public record ReaderPaperResponse(
        String title,
        List<String> authors,
        String year,
        String source,
        String summary
) {
}
