package com.teamproject.report.auth.service;

import com.teamproject.report.auth.dto.AuthResponse;
import com.teamproject.report.auth.dto.UpdateUserRequest;
import com.teamproject.report.auth.dto.UserProfileResponse;
import com.teamproject.report.auth.exception.AuthException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void signUpLoginAndProfileWork() {
        AuthResponse signUp = authService.signUp("user@example.com", "Tester", "password123");
        assertThat(signUp.accessToken()).isNotBlank();

        AuthResponse login = authService.login("user@example.com", "password123");
        assertThat(login.accessToken()).isNotBlank();

        UserProfileResponse profile = authService.getProfile("Bearer " + login.accessToken());
        assertThat(profile.email()).isEqualTo("user@example.com");
        assertThat(profile.name()).isEqualTo("Tester");
    }

    @Test
    void deleteCurrentUserRemovesAccountAndSessions() {
        AuthResponse signUp = authService.signUp("delete@example.com", "Delete Me", "password123");

        authService.deleteCurrentUser("Bearer " + signUp.accessToken());

        assertThatThrownBy(() -> authService.getProfile("Bearer " + signUp.accessToken()))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid access token");
        assertThatThrownBy(() -> authService.login("delete@example.com", "password123"))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void updateCurrentUserChangesProfileAndPassword() {
        AuthResponse signUp = authService.signUp("update@example.com", "Before", "password123");

        UserProfileResponse updated = authService.updateCurrentUser(
                "Bearer " + signUp.accessToken(),
                new UpdateUserRequest("updated@example.com", "After", "newpassword123")
        );

        assertThat(updated.email()).isEqualTo("updated@example.com");
        assertThat(updated.name()).isEqualTo("After");

        AuthResponse login = authService.login("updated@example.com", "newpassword123");
        assertThat(login.accessToken()).isNotBlank();
    }
}
