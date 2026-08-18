package com.zerowiki.ingest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobQuestionRepository extends JpaRepository<JobQuestionEntity, UUID> {
    List<JobQuestionEntity> findAllByIngestJobIdOrderByCreatedAtAsc(UUID ingestJobId);

    Optional<JobQuestionEntity> findByIdAndIngestJobId(UUID id, UUID ingestJobId);
}
