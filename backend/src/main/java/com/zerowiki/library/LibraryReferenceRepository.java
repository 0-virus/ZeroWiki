package com.zerowiki.library;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LibraryReferenceRepository extends JpaRepository<LibraryReferenceEntity, UUID> {
    List<LibraryReferenceEntity> findAllBySourceLibraryIdOrderByCreatedAtDesc(UUID sourceLibraryId);

    Optional<LibraryReferenceEntity> findBySourceLibraryIdAndTargetLibraryId(UUID sourceLibraryId, UUID targetLibraryId);
}
