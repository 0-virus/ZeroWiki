package com.zerowiki.source;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSessionEntity, UUID> {
    Optional<UploadSessionEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<UploadSessionEntity> findByIdAndStatus(UUID id, UploadStatus status);
}
