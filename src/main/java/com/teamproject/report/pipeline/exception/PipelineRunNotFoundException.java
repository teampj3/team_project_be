package com.teamproject.report.pipeline.exception;

public class PipelineRunNotFoundException extends RuntimeException {

    public PipelineRunNotFoundException(String runId) {
        super("Pipeline run not found: " + runId);
    }
}
