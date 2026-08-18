package com.zerowiki.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContradictionRepository extends JpaRepository<ContradictionEntity, UUID> {
    List<ContradictionEntity> findAllByLibraryIdOrderByCreatedAtDesc(UUID libraryId);

    List<ContradictionEntity> findAllByLibraryIdAndStatusOrderByCreatedAtDesc(UUID libraryId, String status);

    List<ContradictionEntity> findAllByLibraryIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID libraryId, Instant since);

    List<ContradictionEntity> findAllByLibraryIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(UUID libraryId,
                                                                                                 String status,
                                                                                                 Instant since);

    Optional<ContradictionEntity> findByIdAndLibraryId(UUID id, UUID libraryId);
}
