package com.teamproject.report.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email @Size(max = 120) String email,
        @Size(max = 50) String name,
        @Size(min = 8, max = 100) String password
) {
    public boolean isEmpty() {
        return isBlank(email) && isBlank(name) && isBlank(password);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
