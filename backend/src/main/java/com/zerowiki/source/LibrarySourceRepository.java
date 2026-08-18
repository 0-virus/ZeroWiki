package com.zerowiki.source;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LibrarySourceRepository extends JpaRepository<LibrarySourceEntity, LibrarySourceId> {
    Optional<LibrarySourceEntity> findByIdLibraryIdAndIdSourceId(UUID libraryId, UUID sourceId);

    List<LibrarySourceEntity> findAllByIdLibraryIdOrderByLinkedAtDesc(UUID libraryId);

    List<LibrarySourceEntity> findAllByIdSourceIdOrderByLinkedAtDesc(UUID sourceId);
}
