package com.teamproject.report.report.model;

import com.teamproject.report.report.dto.AiReportResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Report {

    private final UUID id;
    private final String topic;
    private final Instant createdAt;
    private Instant updatedAt;
    private ReportStatus status;
    private String gptDraft;
    private String claudeDraft;
    private List<String> commonHighlights;
    private List<String> differentHighlights;
    private String reviewResult;
    private String mergedReport;
    private String failureMessage;

    private Report(UUID id, String topic, Instant now) {
        this.id = id;
        this.topic = topic;
        this.createdAt = now;
        this.updatedAt = now;
        this.status = ReportStatus.PENDING;
        this.commonHighlights = List.of();
        this.differentHighlights = List.of();
    }

    public static Report create(String topic) {
        return new Report(UUID.randomUUID(), topic, Instant.now());
    }

    public void complete(AiReportResponse response) {
        this.status = ReportStatus.COMPLETED;
        this.gptDraft = response.gptDraft();
        this.claudeDraft = response.claudeDraft();
        this.commonHighlights = List.copyOf(response.commonHighlights());
        this.differentHighlights = List.copyOf(response.differentHighlights());
        this.reviewResult = response.reviewResult();
        this.mergedReport = response.mergedReport();
        this.failureMessage = null;
        this.updatedAt = Instant.now();
    }

    public void fail(String failureMessage) {
        this.status = ReportStatus.FAILED;
        this.failureMessage = failureMessage;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public String getGptDraft() {
        return gptDraft;
    }

    public String getClaudeDraft() {
        return claudeDraft;
    }

    public List<String> getCommonHighlights() {
        return commonHighlights;
    }

    public List<String> getDifferentHighlights() {
        return differentHighlights;
    }

    public String getReviewResult() {
        return reviewResult;
    }

    public String getMergedReport() {
        return mergedReport;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
}
