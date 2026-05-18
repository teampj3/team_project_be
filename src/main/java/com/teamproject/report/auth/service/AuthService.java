package com.teamproject.report.auth.service;

import com.teamproject.report.archive.repository.ArchiveEntryRepository;
import com.teamproject.report.auth.dto.AuthResponse;
import com.teamproject.report.auth.dto.UpdateUserRequest;
import com.teamproject.report.auth.dto.UserProfileResponse;
import com.teamproject.report.auth.exception.AuthException;
import com.teamproject.report.auth.exception.InvalidUpdateRequestException;
import com.teamproject.report.auth.exception.UserAlreadyExistsException;
import com.teamproject.report.auth.model.UserAccount;
import com.teamproject.report.auth.model.UserSession;
import com.teamproject.report.auth.repository.UserAccountRepository;
import com.teamproject.report.auth.repository.UserSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final ArchiveEntryRepository archiveEntryRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserAccountRepository userAccountRepository,
            UserSessionRepository userSessionRepository,
            ArchiveEntryRepository archiveEntryRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.archiveEntryRepository = archiveEntryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse signUp(String email, String name, String password) {
        if (userAccountRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }

        UserAccount user = userAccountRepository.save(new UserAccount(
                email,
                name,
                passwordEncoder.encode(password)
        ));
        UserSession session = userSessionRepository.save(new UserSession(generateToken(), user));
        return toAuthResponse(user, session);
    }

    @Transactional
    public AuthResponse login(String email, String password) {
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException("Invalid email or password");
        }

        UserSession session = userSessionRepository.save(new UserSession(generateToken(), user));
        return toAuthResponse(user, session);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String bearerToken) {
        UserAccount user = requireCurrentUser(bearerToken);
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public UserProfileResponse updateCurrentUser(String bearerToken, UpdateUserRequest request) {
        if (request.isEmpty()) {
            throw new InvalidUpdateRequestException("At least one field must be provided for update");
        }

        UserAccount user = requireCurrentUser(bearerToken);

        String nextEmail = normalize(request.email());
        String nextName = normalize(request.name());
        String nextPassword = normalize(request.password());

        if (nextEmail != null && !nextEmail.equals(user.getEmail()) && userAccountRepository.existsByEmail(nextEmail)) {
            throw new UserAlreadyExistsException(nextEmail);
        }

        user.updateProfile(
                nextEmail,
                nextName,
                nextPassword == null ? null : passwordEncoder.encode(nextPassword)
        );

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public void deleteCurrentUser(String bearerToken) {
        UserAccount user = requireCurrentUser(bearerToken);
        UUID userId = user.getId();
        archiveEntryRepository.deleteAllByUserId(userId);
        userSessionRepository.deleteAllByUserId(userId);
        userAccountRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public UserAccount requireCurrentUser(String bearerToken) {
        String token = extractToken(bearerToken);
        UserSession session = userSessionRepository.findByAccessToken(token)
                .orElseThrow(() -> new AuthException("Invalid access token"));
        return session.getUser();
    }

    private AuthResponse toAuthResponse(UserAccount user, UserSession session) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                session.getAccessToken(),
                user.getCreatedAt()
        );
    }

    private String extractToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new AuthException("Authorization header must use Bearer token");
        }
        String token = bearerToken.substring(7).trim();
        if (token.isBlank()) {
            throw new AuthException("Authorization token is blank");
        }
        return token;
    }

    private String generateToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
