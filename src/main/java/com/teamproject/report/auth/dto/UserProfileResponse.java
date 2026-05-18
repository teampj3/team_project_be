package com.teamproject.report.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String email,
        String name,
        Instant createdAt
) {
}
