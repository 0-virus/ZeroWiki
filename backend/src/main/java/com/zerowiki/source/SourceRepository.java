package com.zerowiki.source;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceRepository extends JpaRepository<SourceEntity, UUID> {
    Optional<SourceEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<SourceEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    List<SourceEntity> findAllByOwnerIdAndSourceTypeOrderByUpdatedAtDesc(UUID ownerId, SourceType sourceType);

    @Query(value = """
            SELECT s.id FROM sources s
            JOIN library_sources ls ON ls.source_id = s.id
            WHERE s.owner_id = :ownerId AND ls.library_id = :libraryId
              AND to_tsvector('simple', coalesce(s.title, '') || ' ' || coalesce(s.canonical_uri, ''))
                  @@ plainto_tsquery('simple', :query)
            """, nativeQuery = true)
    List<UUID> findKeywordSourceIds(@Param("ownerId") UUID ownerId, @Param("libraryId") UUID libraryId,
                                    @Param("query") String query);
}
