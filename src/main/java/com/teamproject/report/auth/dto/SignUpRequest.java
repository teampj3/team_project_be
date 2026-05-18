package com.teamproject.report.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}
