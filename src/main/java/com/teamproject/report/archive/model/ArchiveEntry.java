package com.teamproject.report.archive.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamproject.report.auth.model.UserAccount;
import com.teamproject.report.report.dto.ReportResponse;
import com.teamproject.report.report.model.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "archive_entries")
public class ArchiveEntry {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false)
    private UUID reportId;

    @Column(length = 100)
    private String runId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(columnDefinition = "TEXT")
    private String gptDraft;

    @Column(columnDefinition = "TEXT")
    private String claudeDraft;

    @Column(columnDefinition = "TEXT")
    private String commonHighlightsJson;

    @Column(columnDefinition = "TEXT")
    private String differentHighlightsJson;

    @Column(columnDefinition = "TEXT")
    private String reviewResult;

    @Column(columnDefinition = "TEXT")
    private String mergedReport;

    @Column(columnDefinition = "TEXT")
    private String failureMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ArchiveEntry() {
    }

    public ArchiveEntry(UserAccount user, UUID reportId, String runId, String title, ReportResponse report) {
        this.user = user;
        this.reportId = reportId;
        this.runId = runId;
        this.title = title;
        applyReport(report);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateFrom(String runId, String title, ReportResponse report) {
        this.runId = runId;
        this.title = title;
        applyReport(report);
        this.updatedAt = Instant.now();
    }

    private void applyReport(ReportResponse report) {
        this.topic = report.topic();
        this.status = report.status();
        this.gptDraft = report.gptDraft();
        this.claudeDraft = report.claudeDraft();
        this.commonHighlightsJson = toJsonArray(report.commonHighlights());
        this.differentHighlightsJson = toJsonArray(report.differentHighlights());
        this.reviewResult = report.reviewResult();
        this.mergedReport = report.mergedReport();
        this.failureMessage = report.failureMessage();
    }

    private String toJsonArray(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize archive highlights", e);
        }
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public UUID getReportId() {
        return reportId;
    }

    public String getRunId() {
        return runId;
    }

    public String getTitle() {
        return title;
    }

    public String getTopic() {
        return topic;
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

    public String getCommonHighlightsJson() {
        return commonHighlightsJson;
    }

    public String getDifferentHighlightsJson() {
        return differentHighlightsJson;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
