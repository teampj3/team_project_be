package com.teamproject.report.pipeline.exception;

public class PipelineStartException extends RuntimeException {

    public PipelineStartException(String message, Throwable cause) {
        super(message, cause);
    }

    public PipelineStartException(String message) {
        super(message);
    }
}
