package com.zerowiki.library;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LibraryRepository extends JpaRepository<LibraryEntity, UUID> {
    Optional<LibraryEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<LibraryEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}
