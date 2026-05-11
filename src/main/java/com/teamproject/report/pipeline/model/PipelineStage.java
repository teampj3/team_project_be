package com.teamproject.report.pipeline.model;

public enum PipelineStage {
    SEARCH("search"),
    READER("reader"),
    RELEVANCE("relevance"),
    WRITER("writer");

    private final String value;

    PipelineStage(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static PipelineStage fromStatus(String currentStage, String failedStage) {
        String token = failedStage != null && !failedStage.isBlank() ? failedStage : currentStage;
        if (token == null || token.isBlank()) {
            return SEARCH;
        }

        String normalized = token.trim().toLowerCase();
        if (normalized.startsWith("reader")) {
            return READER;
        }
        if (normalized.startsWith("relevance")) {
            return RELEVANCE;
        }
        if (normalized.startsWith("writer") || normalized.equals("completed")) {
            return WRITER;
        }
        return SEARCH;
    }
}
