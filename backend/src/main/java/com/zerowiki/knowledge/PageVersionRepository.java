package com.zerowiki.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageVersionRepository extends JpaRepository<PageVersionEntity, UUID> {
    List<PageVersionEntity> findAllByPageIdOrderByVersionNoDesc(UUID pageId);

    Optional<PageVersionEntity> findByPageIdAndVersionNo(UUID pageId, int versionNo);

    Optional<PageVersionEntity> findTopByPageIdOrderByVersionNoDesc(UUID pageId);
}
