package com.zerowiki.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LibraryVersionRepository extends JpaRepository<LibraryVersionEntity, UUID> {
    List<LibraryVersionEntity> findAllByLibraryIdOrderByVersionNoDesc(UUID libraryId);

    Optional<LibraryVersionEntity> findByLibraryIdAndVersionNo(UUID libraryId, long versionNo);

    Optional<LibraryVersionEntity> findByIdAndLibraryId(UUID id, UUID libraryId);

    Optional<LibraryVersionEntity> findTopByLibraryIdOrderByVersionNoDesc(UUID libraryId);
}
