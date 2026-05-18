package com.teamproject.report.auth.controller;

import com.teamproject.report.auth.dto.AuthResponse;
import com.teamproject.report.auth.dto.LoginRequest;
import com.teamproject.report.auth.dto.SignUpRequest;
import com.teamproject.report.auth.dto.UpdateUserRequest;
import com.teamproject.report.auth.dto.UserProfileResponse;
import com.teamproject.report.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request.email(), request.name(), request.password());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @GetMapping("/me")
    public UserProfileResponse me(@RequestHeader("Authorization") String authorization) {
        return authService.getProfile(authorization);
    }

    @PatchMapping("/me")
    public UserProfileResponse updateMe(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return authService.updateCurrentUser(authorization, request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@RequestHeader("Authorization") String authorization) {
        authService.deleteCurrentUser(authorization);
    }
}
