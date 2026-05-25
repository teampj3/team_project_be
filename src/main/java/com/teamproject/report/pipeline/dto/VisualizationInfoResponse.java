package com.teamproject.report.pipeline.dto;

import java.util.Map;

public record VisualizationInfoResponse(
        String manifestPath,
        Map<String, String> assets,
        String visualizedReportPath
) {
    public VisualizationInfoResponse {
        assets = assets == null ? Map.of() : Map.copyOf(assets);
    }
}
