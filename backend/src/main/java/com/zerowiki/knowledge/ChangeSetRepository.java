package com.zerowiki.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface ChangeSetRepository extends JpaRepository<ChangeSetEntity, UUID> {
    List<ChangeSetEntity> findAllByLibraryIdOrderByCreatedAtDesc(UUID libraryId);

    Optional<ChangeSetEntity> findByIdAndLibraryId(UUID id, UUID libraryId);

    Optional<ChangeSetEntity> findByLibraryIdAndOriginTypeAndOriginId(UUID libraryId, String originType, UUID originId);
}
