package com.zerowiki.auth;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    JwtTokenService tokenService;
    @Mock
    AuthSessionRepository sessionRepository;
    @Mock
    FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void revoked_session_does_not_authenticate_access_token() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(tokenService.parseAccessToken(eq("token"), any(Instant.class)))
                .thenReturn(new JwtTokenService.AccessClaims(userId, sessionId, Instant.now().plusSeconds(60)));
        when(sessionRepository.existsActiveByIdAndUserId(eq(sessionId), eq(userId), any(Instant.class), eq(UserStatus.ACTIVE)))
                .thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        new JwtAuthenticationFilter(tokenService, sessionRepository)
                .doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void active_session_authenticates_matching_access_token() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(tokenService.parseAccessToken(eq("token"), any(Instant.class)))
                .thenReturn(new JwtTokenService.AccessClaims(userId, sessionId, Instant.now().plusSeconds(60)));
        when(sessionRepository.existsActiveByIdAndUserId(eq(sessionId), eq(userId), any(Instant.class), eq(UserStatus.ACTIVE)))
                .thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        new JwtAuthenticationFilter(tokenService, sessionRepository)
                .doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(userId.toString());
        verify(filterChain).doFilter(any(), any());
    }
}
