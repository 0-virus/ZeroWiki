package com.zerowiki.ingest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngestJobRepository extends JpaRepository<IngestJobEntity, UUID> {
    Optional<IngestJobEntity> findByIdAndLibraryIdAndRequestedBy(UUID id, UUID libraryId, UUID requestedBy);

    List<IngestJobEntity> findAllByLibraryIdAndRequestedByOrderByCreatedAtDesc(UUID libraryId, UUID requestedBy);

    Optional<IngestJobEntity> findFirstByStatusOrderByCreatedAtAsc(IngestStatus status);
}
