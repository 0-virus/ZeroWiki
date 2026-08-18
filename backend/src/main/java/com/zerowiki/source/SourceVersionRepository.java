package com.zerowiki.source;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceVersionRepository extends JpaRepository<SourceVersionEntity, UUID> {
    List<SourceVersionEntity> findAllBySourceIdOrderByVersionNoDesc(UUID sourceId);

    Optional<SourceVersionEntity> findBySourceIdAndVersionNo(UUID sourceId, int versionNo);

    Optional<SourceVersionEntity> findTopBySourceIdOrderByVersionNoDesc(UUID sourceId);

    List<SourceVersionEntity> findAllByIdIn(List<UUID> ids);
}
