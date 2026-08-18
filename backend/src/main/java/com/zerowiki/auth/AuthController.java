package com.zerowiki.auth;

import com.zerowiki.common.ApiResponse;
import com.zerowiki.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping
public class AuthController {
    private final AuthService authService;
    private final AuthProperties authProperties;

    public AuthController(AuthService authService, AuthProperties authProperties) {
        this.authService = authService;
        this.authProperties = authProperties;
    }

    @PostMapping("/auth/sign-up")
    ResponseEntity<ApiResponse<AuthDtos.UserResponse>> signUp(@Valid @RequestBody AuthDtos.SignUpRequest request) {
        UserEntity user = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(AuthDtos.UserResponse.from(user)));
    }

    @PostMapping("/auth/login")
    ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthDtos.AuthSessionResult result = authService.login(request);
        return withRefreshCookie(HttpStatus.OK, new AuthResponseBody(
                new AuthDtos.AuthResponse(result.accessToken(), authProperties.getAccessTokenSeconds(),
                        AuthDtos.UserResponse.from(result.user())), result.refreshToken()));
    }

    @PostMapping("/auth/refresh")
    ResponseEntity<ApiResponse<AuthDtos.RefreshResponse>> refresh(HttpServletRequest request) {
        AuthDtos.AuthSessionResult result = authService.refresh(refreshCookie(request));
        return withRefreshCookie(HttpStatus.OK, new AuthResponseBody(
                new AuthDtos.RefreshResponse(result.accessToken(), authProperties.getAccessTokenSeconds()),
                result.refreshToken()));
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(refreshCookie(request));
        return clearCookie();
    }

    @PostMapping("/auth/logout-all")
    ResponseEntity<Void> logoutAll(Authentication authentication) {
        authService.logoutAll(userId(authentication));
        return clearCookie();
    }

    @GetMapping("/me")
    ApiResponse<AuthDtos.UserResponse> me(Authentication authentication) {
        return new ApiResponse<>(AuthDtos.UserResponse.from(authService.getUser(userId(authentication))));
    }

    @PatchMapping("/me")
    ApiResponse<AuthDtos.UserResponse> updateMe(Authentication authentication,
                                                 @Valid @RequestBody AuthDtos.UpdateMeRequest request) {
        return new ApiResponse<>(AuthDtos.UserResponse.from(authService.updateUser(userId(authentication), request)));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private String refreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (authProperties.getRefreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private <T> ResponseEntity<ApiResponse<T>> withRefreshCookie(HttpStatus status, AuthResponseBody body) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(body.refreshToken()).toString())
                .body(new ApiResponse<>((T) body.response()));
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(authProperties.getRefreshCookieName(), value)
                .httpOnly(true)
                .secure(authProperties.isRefreshCookieSecure())
                .sameSite("Lax")
                .path("/auth")
                .maxAge(Duration.ofDays(authProperties.getRefreshTokenDays()))
                .build();
    }

    private ResponseEntity<Void> clearCookie() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, ResponseCookie.from(authProperties.getRefreshCookieName(), "")
                        .httpOnly(true)
                        .secure(authProperties.isRefreshCookieSecure())
                        .sameSite("Lax")
                        .path("/auth")
                        .maxAge(Duration.ZERO)
                        .build().toString())
                .build();
    }

    private record AuthResponseBody(Object response, String refreshToken) {
    }
}
