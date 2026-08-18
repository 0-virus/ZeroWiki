package com.zerowiki.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, UUID> {
    Optional<AuthSessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    List<AuthSessionEntity> findAllByUserIdAndRevokedAtIsNull(UUID userId);

    @Query("""
            select case when count(s) > 0 then true else false end
            from AuthSessionEntity s
            join s.user u
            where s.id = :sessionId
              and u.id = :userId
              and s.revokedAt is null
              and s.expiresAt > :now
              and u.status = :status
            """)
    boolean existsActiveByIdAndUserId(@Param("sessionId") UUID sessionId,
                                      @Param("userId") UUID userId,
                                      @Param("now") Instant now,
                                      @Param("status") UserStatus status);
}
