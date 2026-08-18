package com.zerowiki.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<ClaimEntity, UUID> {
    List<ClaimEntity> findAllByPageIdOrderByCreatedAtAsc(UUID pageId);

    Optional<ClaimEntity> findByIdAndPageId(UUID id, UUID pageId);

    Optional<ClaimEntity> findByPageIdAndClaimKey(UUID pageId, String claimKey);

    @Query(value = """
            SELECT c.id FROM claims c
            JOIN pages p ON p.id = c.page_id
            WHERE p.library_id = :libraryId
              AND to_tsvector('simple', coalesce(c.statement, ''))
                  @@ plainto_tsquery('simple', :query)
            """, nativeQuery = true)
    List<UUID> findKeywordClaimIds(@Param("libraryId") UUID libraryId, @Param("query") String query);
}
