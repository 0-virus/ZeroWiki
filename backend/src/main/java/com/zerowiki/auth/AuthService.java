package com.zerowiki.auth;

import com.zerowiki.common.ApiException;
import com.zerowiki.config.AuthProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final AuthProperties authProperties;

    public AuthService(UserRepository userRepository, AuthSessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder, JwtTokenService tokenService,
                       AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authProperties = authProperties;
    }

    @Transactional
    public UserEntity signUp(AuthDtos.SignUpRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "이미 가입된 이메일입니다.");
        }
        UserEntity user = saveUser(email, request.password(), request.displayName());
        return user;
    }

    @Transactional
    public AuthDtos.AuthSessionResult login(AuthDtos.LoginRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> invalidCredentials());
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return issueSession(user, Instant.now());
    }

    @Transactional
    public AuthDtos.AuthSessionResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw invalidCredentials();
        }
        Instant now = Instant.now();
        AuthSessionEntity current = sessionRepository.findByRefreshTokenHash(tokenService.hashRefreshToken(refreshToken))
                .orElseThrow(this::invalidCredentials);
        if (!current.isUsable(now)) {
            throw invalidCredentials();
        }
        current.revoke(now);
        return issueSession(current.getUser(), now);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        sessionRepository.findByRefreshTokenHash(tokenService.hashRefreshToken(refreshToken))
                .ifPresent(session -> session.revoke(Instant.now()));
    }

    @Transactional
    public void logoutAll(UUID userId) {
        Instant now = Instant.now();
        sessionRepository.findAllByUserIdAndRevokedAtIsNull(userId).forEach(session -> session.revoke(now));
    }

    @Transactional(readOnly = true)
    public UserEntity getUser(UUID userId) {
        return userRepository.findById(userId).filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public UserEntity updateUser(UUID userId, AuthDtos.UpdateMeRequest request) {
        UserEntity user = getUser(userId);
        user.updateProfile(request.displayName(), request.locale());
        return user;
    }

    private UserEntity saveUser(String email, String password, String displayName) {
        try {
            return userRepository.save(UserEntity.create(email, passwordEncoder.encode(password), displayName.trim()));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "이미 가입된 이메일입니다.");
        }
    }

    private AuthDtos.AuthSessionResult issueSession(UserEntity user, Instant now) {
        String refreshToken = tokenService.newRefreshToken();
        AuthSessionEntity session = AuthSessionEntity.create(user, tokenService.hashRefreshToken(refreshToken),
                now.plus(authProperties.getRefreshTokenDays(), ChronoUnit.DAYS));
        sessionRepository.save(session);
        String accessToken = tokenService.createAccessToken(user.getId(), session.getId(), now,
                authProperties.getAccessTokenSeconds());
        return new AuthDtos.AuthSessionResult(accessToken, refreshToken, user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
