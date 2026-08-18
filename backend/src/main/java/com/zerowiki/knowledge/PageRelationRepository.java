package com.zerowiki.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface PageRelationRepository extends JpaRepository<PageRelationEntity, UUID> {
    List<PageRelationEntity> findAllByLibraryIdOrderByCreatedAtDesc(UUID libraryId);

    List<PageRelationEntity> findAllByLibraryIdAndStatusOrderByCreatedAtDesc(UUID libraryId, String status);

    List<PageRelationEntity> findAllByLibraryIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID libraryId, Instant since);

    List<PageRelationEntity> findAllByLibraryIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(UUID libraryId,
                                                                                               String status,
                                                                                               Instant since);

    Optional<PageRelationEntity> findByLibraryIdAndSourcePageIdAndTargetPageIdAndRelationType(
            UUID libraryId, UUID sourcePageId, UUID targetPageId, String relationType);
}
