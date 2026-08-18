package com.zerowiki.auth;

import com.zerowiki.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    AuthSessionRepository sessionRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtTokenService tokenService;

    private final AuthProperties authProperties = new AuthProperties();

    @Test
    void refresh_rotates_token_and_revokes_previous_session() {
        UserEntity user = UserEntity.create("user@example.com", "hash", "제로");
        AuthSessionEntity previous = AuthSessionEntity.create(user, "old-hash", Instant.now().plusSeconds(3600));
        when(tokenService.hashRefreshToken("old-token")).thenReturn("old-hash");
        when(sessionRepository.findByRefreshTokenHash("old-hash")).thenReturn(Optional.of(previous));
        when(tokenService.newRefreshToken()).thenReturn("new-token");
        when(tokenService.hashRefreshToken("new-token")).thenReturn("new-hash");
        when(sessionRepository.save(any(AuthSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenService.createAccessToken(any(), any(), any(), anyLong())).thenReturn("access-token");

        AuthService service = new AuthService(userRepository, sessionRepository, passwordEncoder, tokenService, authProperties);
        AuthDtos.AuthSessionResult result = service.refresh("old-token");

        assertThat(result.refreshToken()).isEqualTo("new-token");
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(previous.getRevokedAt()).isNotNull();
        verify(sessionRepository, times(1)).save(any(AuthSessionEntity.class));
    }

    @Test
    void logout_is_idempotent_when_cookie_is_missing() {
        AuthService service = new AuthService(userRepository, sessionRepository, passwordEncoder, tokenService, authProperties);
        assertThatCode(() -> service.logout(null)).doesNotThrowAnyException();
        verifyNoInteractions(tokenService, sessionRepository);
    }
}
