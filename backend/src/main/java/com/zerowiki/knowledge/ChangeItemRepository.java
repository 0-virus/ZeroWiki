package com.zerowiki.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface ChangeItemRepository extends JpaRepository<ChangeItemEntity, UUID> {
    List<ChangeItemEntity> findAllByChangeSetIdOrderByCreatedAtAsc(UUID changeSetId);

    Optional<ChangeItemEntity> findByIdAndChangeSetId(UUID id, UUID changeSetId);
}
