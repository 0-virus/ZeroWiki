package com.zerowiki.assistant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {
    Optional<IdempotencyRecordEntity> findByUserIdAndRequestPathAndIdempotencyKey(
            UUID userId, String requestPath, String idempotencyKey);
}
