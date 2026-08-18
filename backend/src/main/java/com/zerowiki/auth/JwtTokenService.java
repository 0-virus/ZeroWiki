package com.zerowiki.auth;

import com.zerowiki.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenService(JwtEncoder encoder, JwtDecoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public String createAccessToken(UUID userId, UUID sessionId, Instant now, long lifetimeSeconds) {
        Instant expiresAt = now.plusSeconds(lifetimeSeconds);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .claim("sessionId", sessionId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public AccessClaims parseAccessToken(String token, Instant now) {
        try {
            Jwt jwt = decoder.decode(token);
            Instant expiresAt = jwt.getExpiresAt();
            String subject = jwt.getSubject();
            String sessionId = jwt.getClaimAsString("sessionId");
            if (expiresAt == null || !expiresAt.isAfter(now)
                    || subject == null || sessionId == null) {
                throw invalidToken();
            }
            return new AccessClaims(UUID.fromString(subject), UUID.fromString(sessionId), expiresAt);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidToken();
        }
    }

    public String newRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashRefreshToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Refresh token hashing failed", exception);
        }
    }

    private ApiException invalidToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증 정보가 유효하지 않습니다.");
    }

    public record AccessClaims(UUID userId, UUID sessionId, Instant expiresAt) {
    }
}
