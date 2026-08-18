package com.zerowiki.assistant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {
    List<ConversationEntity> findAllByLibraryIdAndUserIdOrderByUpdatedAtDesc(UUID libraryId, UUID userId);

    Optional<ConversationEntity> findByIdAndLibraryIdAndUserId(UUID id, UUID libraryId, UUID userId);
}
