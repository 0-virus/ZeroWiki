package com.zerowiki.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record SignUpRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank @Size(max = 80) String displayName) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UpdateMeRequest(@Size(max = 80) String displayName, @Size(max = 10) String locale) {
    }

    public record UserResponse(UUID id, String email, String displayName, String locale,
                               UserStatus status, String plan, Instant createdAt) {
        static UserResponse from(UserEntity user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getLocale(),
                    user.getStatus(), "FREE", user.getCreatedAt());
        }
    }

    public record AuthResponse(String accessToken, long accessTokenExpiresIn, UserResponse user) {
    }

    public record RefreshResponse(String accessToken, long accessTokenExpiresIn) {
    }

    public record AuthSessionResult(String accessToken, String refreshToken, UserEntity user) {
    }
}
