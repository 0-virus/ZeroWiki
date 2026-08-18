package com.zerowiki.ingest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IngestItemRepository extends JpaRepository<IngestItemEntity, UUID> {
    List<IngestItemEntity> findAllByIngestJobIdOrderByCreatedAtAsc(UUID ingestJobId);

    List<IngestItemEntity> findAllByIngestJobIdAndStatus(UUID ingestJobId, IngestItemStatus status);
}
