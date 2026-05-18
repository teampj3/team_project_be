package com.teamproject.report.archive.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SaveArchiveRequest(
        @NotNull UUID reportId,
        @Size(max = 200) String title
) {
}
