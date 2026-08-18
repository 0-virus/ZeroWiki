package com.zerowiki.library;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LibraryConstitutionRepository extends JpaRepository<LibraryConstitutionEntity, UUID> {
    Optional<LibraryConstitutionEntity> findByIdAndLibraryId(UUID id, UUID libraryId);

    Optional<LibraryConstitutionEntity> findByLibraryIdAndVersionNo(UUID libraryId, int versionNo);

    List<LibraryConstitutionEntity> findAllByLibraryIdOrderByVersionNoDesc(UUID libraryId);
}
