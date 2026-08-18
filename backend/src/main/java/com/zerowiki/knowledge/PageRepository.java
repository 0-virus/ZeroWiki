package com.zerowiki.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageRepository extends JpaRepository<PageEntity, UUID> {
    List<PageEntity> findAllByLibraryIdOrderByUpdatedAtDesc(UUID libraryId);

    Optional<PageEntity> findByIdAndLibraryId(UUID id, UUID libraryId);

    Optional<PageEntity> findByLibraryIdAndSlug(UUID libraryId, String slug);

    @Query(value = """
            SELECT p.id FROM pages p
            JOIN page_versions v ON v.id = p.current_version_id
            WHERE p.library_id = :libraryId
              AND to_tsvector('simple', coalesce(v.title, '') || ' ' || coalesce(v.markdown_body, ''))
                  @@ plainto_tsquery('simple', :query)
            """, nativeQuery = true)
    List<UUID> findKeywordPageIds(@Param("libraryId") UUID libraryId, @Param("query") String query);
}
